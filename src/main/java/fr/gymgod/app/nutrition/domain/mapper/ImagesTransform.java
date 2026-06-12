package fr.gymgod.app.nutrition.domain.mapper;

import fr.gymgod.app.nutrition.domain.entites.record.ImagesRecord;
import fr.gymgod.common.constants.ConstantsCommon;
import fr.gymgod.common.entities.nutrition.Images;
import org.springframework.stereotype.Service;

@Service
public class ImagesTransform {

    public ImagesRecord fromImages(Images images) {
        String imageUrl;

        if(images.isImageUrlDownload()){
            imageUrl = getURI(images.getImageServerUrl());
        } else {
            imageUrl = images.getImageUrl();
        }

        String imageIngredientUrl;

        if(images.isImageIngredientUrlDownload()){
            imageIngredientUrl = getURI(images.getImageIngredientServerUrl());
            } else {
            imageIngredientUrl = images.getImageIngredientUrl();
        }

        String imageNutritionUrl;

        if(images.isImageNutritionUrlDownload()){
            imageNutritionUrl = getURI(images.getImageNutritionServerUrl());
        } else {
            imageNutritionUrl = images.getImageNutritionUrl();
        }

        return new ImagesRecord(
                images.getId(),
                imageUrl,
                imageIngredientUrl,
                imageNutritionUrl
        );
    }

    private String getURI(String imageUrl){
        return "http://localhost:8080".concat(ConstantsCommon.ENDPOINT_NUTRITION).concat("/image/").concat(imageUrl);
    }
}
