package fr.gymgod.app.nutrition.service;

import fr.gymgod.app.nutrition.domain.port.NutritionDataPort;
import fr.gymgod.app.nutrition.domain.entites.record.ProductRecord;
import fr.gymgod.app.nutrition.domain.mapper.ProductTransform;
import fr.gymgod.common.entities.nutrition.Product;
import fr.gymgod.common.entities.user.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrchestratorNutrition {

    private final NutritionDataPort nutritionDataPort;
    private final ProductTransform productTransform;

    public ProductRecord getProduct(String key) {
        Product product = this.nutritionDataPort.getProduct(key);
        return this.productTransform.fromProduct(product);
    }

    public List<ProductRecord> searchProducts(String name, int page, int size) {
        // 1. Historique utilisateur (Top 10) — uniquement sur page 0, et uniquement si
        //    un utilisateur réel est authentifié. L'endpoint /nutrition/search est public :
        //    un appel anonyme (pas de JSESSIONID) ne doit pas provoquer de 500.
        List<Product> historyProducts = new ArrayList<>();
        if (page == 0 && isAuthenticatedUser()) {
            try {
                UserAccount user = nutritionDataPort.getCurrentUser();
                historyProducts = nutritionDataPort.searchHistoryProducts(user, name, 10);
            } catch (Exception ignored) {
                // Session expirée entre le check et l'appel — l'historique est un bonus,
                // pas un prérequis : on continue avec la recherche globale seule.
            }
        }

        // 2. Recherche globale
        List<Product> globalProducts = this.nutritionDataPort.searchGlobalProducts(name,
                org.springframework.data.domain.PageRequest.of(page, size));

        // 3. Fusion et déduplication (priorité à l'historique)
        Set<String> addedCodes = new HashSet<>();
        List<Product> finalProducts = new ArrayList<>();

        for (Product p : historyProducts) {
            if (addedCodes.add(p.getCode())) {
                finalProducts.add(p);
            }
        }
        for (Product p : globalProducts) {
            if (addedCodes.add(p.getCode())) {
                finalProducts.add(p);
            }
        }

        // fromProductForSearch évite le problème N+1 : ne touche pas les
        // associations ManyToMany (categorie, label, ingredient, allergen…),
        // qui déclencheraient sinon 7 × taille-de-page requêtes SQL.
        return finalProducts.stream()
                .map(this.productTransform::fromProductForSearch)
                .collect(Collectors.toList());
    }

    /**
     * Renvoie {@code true} si l'utilisateur courant est un utilisateur réel (pas
     * anonyme, pas null) — évite de lever une exception dans
     * {@link NutritionDataPort#getCurrentUser()} lors d'un appel public non
     * authentifié sur {@code GET /nutrition/search}.
     */
    private boolean isAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }

    public int cleanupOrphanIngredients(int limit) {
        return nutritionDataPort.cleanupOrphanIngredients(limit);
    }

    public String deduplicateIngredients() {
        return nutritionDataPort.deduplicateIngredients();
    }
}
