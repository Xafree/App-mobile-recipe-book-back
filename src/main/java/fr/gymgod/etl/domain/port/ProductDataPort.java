package fr.gymgod.etl.domain.port;

import fr.gymgod.common.entities.nutrition.Product;
import java.util.List;

/**
 * Port used by the domain to fetch and save product data.
 * The implementation detail is hidden from the core business logic.
 */
public interface ProductDataPort {

    List<Product> getAllByCode(List<String> codes);

    List<Product> saveAll(List<Product> products);

    Product get(String code);

    Product save(Product product);

    List<Product> getPendingAiEnrichment(int limit);

    List<Product> getPendingReferenceEnrichment(int limit);
}
