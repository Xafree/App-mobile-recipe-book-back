package fr.gymgod.app.nutrition.gateway;

import fr.gymgod.app.nutrition.domain.port.NutritionDataPort;
import fr.gymgod.common.domain.nutrition.MealRepository;
import fr.gymgod.common.domain.nutrition.ProductRepository;
import fr.gymgod.common.entities.nutrition.Product;
import fr.gymgod.common.entities.user.UserAccount;
import fr.gymgod.common.service.SessionSpringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NutritionGateway implements NutritionDataPort {

    private final ProductRepository productRepository;
    private final MealRepository mealRepository;
    private final SessionSpringService sessionSpringService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Product getProduct(String key) {
        return productRepository.findById(key).orElse(null);
    }

    @Override
    public List<Product> searchHistoryProducts(UserAccount user, String name, int limit) {
        return mealRepository.findTopProductsByUserIdAndNameLike(user.getId(), name, PageRequest.of(0, limit));
    }

    @Override
    public List<Product> searchGlobalProducts(String name, Pageable pageable) {
        // findByNameForPicker renvoie une List directement — pas de COUNT(*) :
        // évite la requête de comptage sur 656 k lignes qui doublait le temps de réponse.
        return productRepository.findByNameForPicker(name, pageable);
    }

    @Override
    public UserAccount getCurrentUser() {
        return sessionSpringService.getCurrentUser();
    }

    @Override
    public int cleanupOrphanIngredients(int limit) {
        String sql = """
                DELETE FROM ingredients
                WHERE id IN (
                    SELECT i.id
                    FROM ingredients i
                    LEFT JOIN products_ingredient pi ON i.id = pi.ingredient_id
                    WHERE pi.ingredient_id IS NULL
                    LIMIT ?
                )
                """;

        int deleted = jdbcTemplate.update(sql, limit);
        log.info("Cleaned up {} orphan ingredients.", deleted);
        return deleted;
    }

    @Override
    public String deduplicateIngredients() {
        log.info("Starting ingredient deduplication...");

        String sqlFindDuplicates = """
                    SELECT lower(name)
                    FROM ingredients
                    GROUP BY lower(name)
                    HAVING count(*) > 1
                """;

        List<String> duplicateNames = jdbcTemplate.queryForList(sqlFindDuplicates, String.class);
        log.info("Found {} duplicate ingredient groups.", duplicateNames.size());

        int totalMerged = 0;
        int totalDeleted = 0;

        for (String lowerName : duplicateNames) {
            String sqlGetIds = "SELECT id FROM ingredients WHERE lower(name) = ?";
            List<java.util.UUID> ids = jdbcTemplate.queryForList(sqlGetIds, java.util.UUID.class, lowerName);

            if (ids.size() < 2)
                continue;

            java.util.UUID masterId = ids.get(0);
            List<java.util.UUID> duplicateIds = ids.subList(1, ids.size());

            log.debug("Merging '{}': keeping {}, removing {}", lowerName, masterId, duplicateIds);

            for (java.util.UUID dupId : duplicateIds) {
                String sqlProductsWithDup = "SELECT product_code FROM products_ingredient WHERE ingredient_id = ?";
                List<String> productsWithDup = jdbcTemplate.queryForList(sqlProductsWithDup, String.class, dupId);

                for (String productCode : productsWithDup) {
                    Integer hasMaster = jdbcTemplate.queryForObject(
                            "SELECT count(*) FROM products_ingredient WHERE product_code = ? AND ingredient_id = ?",
                            Integer.class, productCode, masterId);

                    if (hasMaster != null && hasMaster > 0) {
                        jdbcTemplate.update(
                                "DELETE FROM products_ingredient WHERE product_code = ? AND ingredient_id = ?",
                                productCode, dupId);
                    } else {
                        jdbcTemplate.update(
                                "UPDATE products_ingredient SET ingredient_id = ? WHERE product_code = ? AND ingredient_id = ?",
                                masterId, productCode, dupId);
                    }
                }

                jdbcTemplate.update("DELETE FROM ingredients WHERE id = ?", dupId);
                totalDeleted++;
            }
            totalMerged++;
        }

        String result = String.format("Deduplication complete. Merged %d groups, deleted %d duplicate ingredients.",
                totalMerged, totalDeleted);
        log.info(result);
        return result;
    }
}
