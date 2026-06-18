package fr.gymgod.common.entities.nutrition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Aliment brut/générique de référence (CIQUAL ou USDA FoodData Central),
 * utilisé par {@code ReferenceFoodEnrichmentService} pour compléter les
 * {@code Nutriment}/{@code Glucide} des produits OFF dont les macros sont
 * manquantes ({@code nutritionDataIncomplete=true}).
 *
 * <p>Index pg_trgm à créer MANUELLEMENT une fois en base (ddl-auto=update ne
 * gère pas les index GIN), même mécanisme que documenté sur {@link Product} :
 *
 * <pre>
 *   CREATE EXTENSION IF NOT EXISTS pg_trgm;
 *   CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_reference_food_name_trgm
 *       ON reference_foods USING GIN (name gin_trgm_ops);
 * </pre>
 */
@Entity
@Table(name = "reference_foods", uniqueConstraints = @UniqueConstraint(columnNames = { "source", "source_code" }))
@Data
public class ReferenceFood {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReferenceFoodSource source;

    @Column(name = "source_code", nullable = false, length = 50)
    private String sourceCode;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String category;

    @Column(name = "calories_per100g", precision = 6, scale = 2)
    private BigDecimal caloriesPer100g;

    @Column(name = "protein_per100g", precision = 5, scale = 2)
    private BigDecimal proteinPer100g;

    @Column(name = "carbs_per100g", precision = 5, scale = 2)
    private BigDecimal carbsPer100g;

    @Column(name = "fat_per100g", precision = 5, scale = 2)
    private BigDecimal fatPer100g;

    @Column(name = "fiber_per100g", precision = 5, scale = 2)
    private BigDecimal fiberPer100g;

    @Column(name = "sugar_per100g", precision = 5, scale = 2)
    private BigDecimal sugarPer100g;

    @Column(name = "saturated_fat_per100g", precision = 5, scale = 2)
    private BigDecimal saturatedFatPer100g;

    // ── Minéraux exprimés en mg/100g (unité standard CIQUAL/USDA) ───────────
    // Convertis en g/100g lors du mapping vers Glucide.

    @Column(name = "sodium_mg_per100g", precision = 7, scale = 2)
    private BigDecimal sodiumMgPer100g;

    @Column(name = "potassium_mg_per100g", precision = 7, scale = 2)
    private BigDecimal potassiumMgPer100g;

    @Column(name = "calcium_mg_per100g", precision = 7, scale = 2)
    private BigDecimal calciumMgPer100g;

    @Column(name = "iron_mg_per100g", precision = 5, scale = 2)
    private BigDecimal ironMgPer100g;
}
