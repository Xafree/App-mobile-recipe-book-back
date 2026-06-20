package fr.gymgod.app.nutrition.service;

import fr.gymgod.app.nutrition.domain.BarcodeNormalizer;
import fr.gymgod.app.nutrition.domain.port.NutritionDataPort;
import fr.gymgod.app.nutrition.domain.entites.record.OcrNutritionRecord;
import fr.gymgod.app.nutrition.domain.entites.record.ProductRecord;
import fr.gymgod.app.nutrition.domain.mapper.OcrNutritionTransform;
import fr.gymgod.app.nutrition.domain.mapper.ProductTransform;
import fr.gymgod.common.entities.nutrition.Nutriment;
import fr.gymgod.common.entities.nutrition.Product;
import fr.gymgod.common.entities.user.UserAccount;
import fr.gymgod.etl.domain.port.OcrLabelParsingPort;
import fr.gymgod.etl.service.NutritionEnrichmentService;
import fr.gymgod.etl.service.OffProductImportService;
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
    private final NutritionEnrichmentService nutritionEnrichmentService;
    private final OffProductImportService offProductImportService;
    private final OcrLabelParsingPort ocrLabelParsingPort;
    private final OcrNutritionTransform ocrNutritionTransform;

    public ProductRecord getProduct(String key) {
        List<String> candidates = BarcodeNormalizer.candidates(key);

        // 1. Recherche en base locale, en essayant le code tel que fourni
        // puis ses variantes normalisées (zéro UPC-A manquant, chiffre de
        // contrôle EAN-13 omis...).
        for (String candidate : candidates) {
            Product product = this.nutritionDataPort.getProduct(candidate);
            if (product != null) {
                product = this.nutritionEnrichmentService.enrichIfNeeded(product);
                return this.productTransform.fromProduct(product);
            }
        }

        // 2. Absent de la base locale sous toutes ses formes — cas typique
        // d'un scan en magasin d'un produit jamais importé par l'ETL : on
        // tente une récupération en direct sur OpenFoodFacts et on
        // l'enregistre pour les prochains scans.
        for (String candidate : candidates) {
            Product product = this.offProductImportService.importProduct(candidate);
            if (product != null) {
                return this.productTransform.fromProduct(product);
            }
        }

        return this.productTransform.fromProduct(null);
    }

    /**
     * Extrait les valeurs nutritionnelles (énergie, protéines, glucides,
     * lipides pour la portion imprimée, nom du produit) depuis le texte brut
     * OCR d'une étiquette — utilisé pour pré-remplir la saisie manuelle d'un
     * ingrédient.
     *
     * <p>En cas d'échec du LLM, renvoie un {@link OcrNutritionRecord} vide
     * (tous champs {@code null}) plutôt qu'une erreur — le client retombe
     * sur un formulaire vierge.
     */
    public OcrNutritionRecord parseOcrLabel(String rawText) {
        return ocrLabelParsingPort.parseLabel(rawText)
                .map(ocrNutritionTransform::toRecord)
                .orElseGet(ocrNutritionTransform::empty);
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

        // 4. Masquage des produits sans aucune valeur nutritionnelle (énergie,
        //    protéines, glucides, lipides toutes à 0) — souvent des imports OFF
        //    incomplets, inutilisables pour le suivi nutritionnel.
        finalProducts = finalProducts.stream().filter(this::hasNutritionData).collect(Collectors.toList());

        // fromProductForSearch évite le problème N+1 : ne touche pas les
        // associations ManyToMany (categorie, label, ingredient, allergen…),
        // qui déclencheraient sinon 7 × taille-de-page requêtes SQL.
        return finalProducts.stream()
                .map(this.productTransform::fromProductForSearch)
                .collect(Collectors.toList());
    }

    private boolean hasNutritionData(Product p) {
        Nutriment nutriment = p.getNutriment();
        if (nutriment == null) {
            return false;
        }
        return nutriment.getEnergyKcal100g() > 0 || nutriment.getProteins100g() > 0
                || nutriment.getCarbohydrates100g() > 0 || nutriment.getFat100g() > 0;
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
