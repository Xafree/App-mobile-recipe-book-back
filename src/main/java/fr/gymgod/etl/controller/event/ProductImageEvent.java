package fr.gymgod.etl.controller.event;

import fr.gymgod.common.entities.nutrition.Images;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ProductImageEvent extends ApplicationEvent {
    private final String productCode;
    private final Images images;

    public ProductImageEvent(Object source, String productCode, Images images) {
        super(source);
        this.productCode = productCode;
        this.images = images;
    }
}
