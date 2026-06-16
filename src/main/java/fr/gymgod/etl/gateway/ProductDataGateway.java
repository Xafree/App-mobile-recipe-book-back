package fr.gymgod.etl.gateway;

import fr.gymgod.etl.domain.port.ProductDataPort;
import fr.gymgod.common.entities.nutrition.Product;
import fr.gymgod.common.domain.nutrition.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Adapter that implements the ProductDataPort.
 */
@Service
@RequiredArgsConstructor
public class ProductDataGateway implements ProductDataPort {

    private final ProductRepository productRepository;

    @Override
    public List<Product> getAllByCode(List<String> codes) {
        return productRepository.findAllById(codes);
    }

    @Override
    public List<Product> saveAll(List<Product> products) {
        List<Product> saved = productRepository.saveAll(products);
        productRepository.flush();
        return saved;
    }

    @Override
    public Product get(String code) {
        return productRepository.findById(code).orElse(null);
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Override
    public List<Product> getPendingAiEnrichment(int limit) {
        return productRepository.findTop50ByAiEnrichedFalseAndIngredientsTextIsNotNull(
                PageRequest.of(0, limit));
    }

    @Override
    public List<Product> getPendingReferenceEnrichment(int limit) {
        return productRepository.findPendingReferenceEnrichment(PageRequest.of(0, limit));
    }
}
