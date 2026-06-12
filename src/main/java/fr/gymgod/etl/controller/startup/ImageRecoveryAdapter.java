package fr.gymgod.etl.controller.startup;

import fr.gymgod.common.domain.nutrition.ProductRepository;
import fr.gymgod.common.entities.nutrition.Product;
import fr.gymgod.etl.controller.event.ProductImageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImageRecoveryAdapter implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    // Removed @Transactional to avoid long-running transaction and potential
    // Hibernate "Illegal pop" issues during startup
    // Repository calls are transactional by default. Since Images are EAGER loaded
    // in Product, we don't need an open session here.
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting Image Recovery Service...");

        List<Product> pendingProducts = productRepository.findProductsWithPendingImages();

        if (pendingProducts.isEmpty()) {
            log.info("No pending image downloads found.");
            return;
        }

        log.info("Found {} products with pending image downloads. Re-triggering events...", pendingProducts.size());

        for (Product product : pendingProducts) {
            eventPublisher.publishEvent(new ProductImageEvent(this, product.getCode(), product.getImage()));
        }

        log.info("Image recovery events published.");
    }
}
