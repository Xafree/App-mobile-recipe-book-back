package fr.gymgod.app.nutrition.domain.mapper;

import fr.gymgod.app.nutrition.domain.entites.record.ProductRecord;
import fr.gymgod.common.entities.nutrition.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductTransform {

    private final BrandTransform brandTransform;
    private final CategorieTransform categorieTransform;
    private final CountryTransform countryTransform;
    private final LabelTransform labelTransform;
    private final IngredientTransform ingredientTransform;
    private final AllergenTransform allergenTransform;
    private final TraceTransform traceTransform;
    private final NutrimentTransform nutrimentTransform;
    private final GlucideTransform glucideTransform;
    private final VitaminTransform vitaminTransform;
    private final ImagesTransform imagesTransform;
    private final AdditiveTransform additiveTransform; // [NEW]

    public ProductRecord fromProduct(Product product) {
        if(product == null){
            return null;
        }

        return new ProductRecord(
                product.getCode(),
                product.getUrl(),
                product.getCreatedTime(),
                product.getLastModifiedTime(),
                product.getProductName(),
                product.getQuantity(),
                product.getBrand() != null ? brandTransform.fromBrand(product.getBrand()) : null,
                product.getCategorie() != null ? product.getCategorie().stream().map(categorieTransform::fromCategorie).collect(Collectors.toList()) : null,
                product.getCountry() != null ? product.getCountry().stream().map(countryTransform::fromCountry).collect(Collectors.toList()) : null,
                product.getLabel() != null ? product.getLabel().stream().map(labelTransform::fromLabel).collect(Collectors.toList()) : null,
                product.getIngredient() != null ? product.getIngredient().stream().map(ingredientTransform::fromIngredient).collect(Collectors.toList()) : null,
                product.getAllergen() != null ? product.getAllergen().stream().map(allergenTransform::fromAllergen).collect(Collectors.toList()) : null,
                product.getTrace() != null ? product.getTrace().stream().map(traceTransform::fromTrace).collect(Collectors.toList()) : null,
                product.getAdditives() != null ? product.getAdditives().stream().map(additiveTransform::fromAdditive).collect(Collectors.toList()) : null, // [NEW]
                product.getNutriscore(),
                product.getNutriment() != null ? nutrimentTransform.fromNutriment(product.getNutriment()) : null,
                product.getGlucide() != null ? glucideTransform.fromGlucide(product.getGlucide()) : null,
                product.getVitamin() != null ? vitaminTransform.fromVitamin(product.getVitamin()) : null,
                product.getImage() != null ? imagesTransform.fromImages(product.getImage()) : null
        );
    }

    /**
     * Variante allégée de {@link #fromProduct} réservée à la liste de recherche
     * du picker d'ingrédients ({@code GET /nutrition/search}).
     *
     * <p>Ne mappe que les champs réellement consommés par {@code ExternalProduct.fromJson}
     * côté Flutter ({@code code}, {@code productName}, {@code quantity}, {@code nutriment},
     * {@code image}). Toutes les associations {@code ManyToMany} ({@code categorie},
     * {@code country}, {@code label}, {@code ingredient}, {@code allergen}, {@code trace},
     * {@code additives}) sont explicitement ignorées : les accéder déclencherait un
     * chargement Hibernate paresseux pour chaque produit de la page, soit jusqu'à
     * 7 × taille-de-page requêtes SQL supplémentaires (problème N+1).
     *
     * <p>{@code Glucide} et {@code Vitamin} sont aussi omis — ils font partie du
     * chargement {@code EAGER} du {@code OneToOne} par défaut mais ne sont jamais
     * lus par le front lors du parcours de la liste.
     */
    public ProductRecord fromProductForSearch(Product product) {
        if (product == null) return null;

        return new ProductRecord(
                product.getCode(),
                null,   // url — inutile pour la liste
                null,   // createdTime
                null,   // lastModifiedTime
                product.getProductName(),
                product.getQuantity(), // nécessaire pour détecter les liquides côté Flutter
                null,   // brand
                null,   // categorie  ← ManyToMany non chargé → 0 requête N+1
                null,   // country    ← idem
                null,   // label      ← idem
                null,   // ingredient ← idem
                null,   // allergen   ← idem
                null,   // trace      ← idem
                null,   // additives  ← idem
                0,      // nutriscore
                product.getNutriment() != null
                        ? nutrimentTransform.fromNutriment(product.getNutriment())
                        : null,
                null,   // glucide  ← OneToOne EAGER chargé mais non renvoyé
                null,   // vitamin  ← idem
                product.getImage() != null
                        ? imagesTransform.fromImages(product.getImage())
                        : null
        );
    }
}
