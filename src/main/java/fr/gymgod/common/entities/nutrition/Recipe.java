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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Data
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Mapped to the original 'name' column to avoid a migration
    @Column(name = "name", nullable = false)
    private String title;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Boolean isFavorite = false;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String category;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    private Integer servings;

    @Column(name = "prep_time_minutes")
    private Integer prepTimeMinutes;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @Column(name = "calories_per_serving", precision = 8, scale = 2)
    private BigDecimal caloriesPerServing = BigDecimal.ZERO;

    @Column(name = "protein_per_serving", precision = 8, scale = 2)
    private BigDecimal proteinPerServing = BigDecimal.ZERO;

    @Column(name = "carbs_per_serving", precision = 8, scale = 2)
    private BigDecimal carbsPerServing = BigDecimal.ZERO;

    @Column(name = "fat_per_serving", precision = 8, scale = 2)
    private BigDecimal fatPerServing = BigDecimal.ZERO;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<RecipeItem> ingredients = new ArrayList<>();

    /**
     * Blocs de préparation (étapes, cuissons, repos) sérialisés en JSONB.
     * Chaque map contient au minimum {@code "type"} ("step"|"cook"|"rest"),
     * et selon le type : {@code text}, {@code tempCelsius}, {@code durationMin}, {@code note}.
     * Stocké tel quel, pas de parsing côté serveur.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "steps", columnDefinition = "jsonb")
    private List<Map<String, Object>> steps;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public void addIngredient(RecipeItem ingredient) {
        ingredients.add(ingredient);
        ingredient.setRecipe(this);
    }
}
