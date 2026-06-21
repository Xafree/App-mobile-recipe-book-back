package fr.gymgod.etl.controller.event;

import fr.gymgod.common.domain.nutrition.ProductRepository;
import fr.gymgod.common.entities.nutrition.Images;
import fr.gymgod.etl.domain.port.FileDownloaderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductImageEventListener {

    private final FileDownloaderPort httpRequestGateway;
    private final ProductRepository productRepository;

    @Value("${etl.image.download.enabled}")
    private boolean IS_IMAGE_DOWNLOAD_ENABLED;

    @Async("imageDownloadExecutor")
    @EventListener
    @Transactional
    public void handleImageDownload(ProductImageEvent event) {
        if (!IS_IMAGE_DOWNLOAD_ENABLED) {
            return; // Skip download if disabled in config
        }

        String code = event.getProductCode();
        Images img = event.getImages();

        // Optimisation: check if download is needed BEFORE synchronized blocks or
        // anything heavy
        boolean updated = false;

        log.debug("Evaluating image download for product code: {}", code);

        if (img.getImageUrl() != null && !img.isImageUrlDownload()) {
            String fileName = code + "/main.jpg";
            log.info("Attempting to download MAIN image for product {}: {}", code, img.getImageUrl());
            if (httpRequestGateway.extractImage(fileName, img.getImageUrl())) {
                img.setImageUrlDownload(true);
                img.setImageServerUrl(fileName);
                updated = true;
            }
        }
        if (img.getImageIngredientUrl() != null && !img.isImageIngredientUrlDownload()) {
            String fileName = code + "/ingredients.jpg";
            log.info("Attempting to download INGREDIENTS image for product {}: {}", code, img.getImageIngredientUrl());
            if (httpRequestGateway.extractImage(fileName, img.getImageIngredientUrl())) {
                img.setImageIngredientUrlDownload(true);
                img.setImageIngredientServerUrl(fileName);
                updated = true;
            }
        }
        if (img.getImageNutritionUrl() != null && !img.isImageNutritionUrlDownload()) {
            String fileName = code + "/nutrition.jpg";
            log.info("Attempting to download NUTRITION image for product {}: {}", code, img.getImageNutritionUrl());
            if (httpRequestGateway.extractImage(fileName, img.getImageNutritionUrl())) {
                img.setImageNutritionUrlDownload(true);
                img.setImageNutritionServerUrl(fileName);
                updated = true;
            }
        }

        if (updated) {
            productRepository.findById(code).ifPresent(p -> {
                p.setImage(img);
                productRepository.save(p);
            });
        }
    }
}
