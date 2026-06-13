package fr.gymgod.common.entities.nutrition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "recipe_ingredient")
@Data
public class RecipeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    @JsonIgnore
    private Recipe recipe;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, length = 10)
    private String unit;

    @Column(name = "calories_per100g", nullable = false, precision = 6, scale = 2)
    private BigDecimal caloriesPer100g;

    @Column(name = "protein_per100g", nullable = false, precision = 5, scale = 2)
    private BigDecimal proteinPer100g;

    @Column(name = "carbs_per100g", nullable = false, precision = 5, scale = 2)
    private BigDecimal carbsPer100g;

    @Column(name = "fat_per100g", nullable = false, precision = 5, scale = 2)
    private BigDecimal fatPer100g;

    @Column(name = "fiber_per100g", precision = 5, scale = 2)
    private BigDecimal fiberPer100g;

    @Column(name = "sugar_per100g", precision = 5, scale = 2)
    private BigDecimal sugarPer100g;

    @Column(name = "saturated_fat_per100g", precision = 5, scale = 2)
    private BigDecimal saturatedFatPer100g;

    @Column(name = "trans_fat_per100g", precision = 5, scale = 2)
    private BigDecimal transFatPer100g;

    @Column(name = "sodium_per100g", precision = 7, scale = 2)
    private BigDecimal sodiumPer100g;

    @Column(name = "external_food_code", length = 64)
    private String externalFoodCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "external_product_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> externalProductSnapshot;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
