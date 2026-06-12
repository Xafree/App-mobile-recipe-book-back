package fr.gymgod.common.entities.nutrition;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "images")
@Data
public class Images {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    //image_url
    @Column(columnDefinition = "TEXT")
    private String imageUrl;
    @Column(columnDefinition = "TEXT")
    private String imageServerUrl;
    private boolean isImageUrlDownload;

    //image_ingredients_url
    @Column(columnDefinition = "TEXT")
    private String imageIngredientUrl;
    @Column(columnDefinition = "TEXT")
    private String imageIngredientServerUrl;
    private boolean isImageIngredientUrlDownload;

    //image_nutrition_url
    @Column(columnDefinition = "TEXT")
    private String imageNutritionUrl;
    @Column(columnDefinition = "TEXT")
    private String imageNutritionServerUrl;
    private boolean isImageNutritionUrlDownload;
}
