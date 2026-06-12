package fr.gymgod.app.nutrition.domain.port;

import fr.gymgod.common.entities.nutrition.Product;
import fr.gymgod.common.entities.user.UserAccount;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NutritionDataPort {
    Product getProduct(String key);

    List<Product> searchHistoryProducts(UserAccount user, String name, int limit);

    List<Product> searchGlobalProducts(String name, Pageable pageable);

    UserAccount getCurrentUser();

    int cleanupOrphanIngredients(int limit);

    String deduplicateIngredients();
}
