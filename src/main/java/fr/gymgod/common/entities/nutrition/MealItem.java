package fr.gymgod.common.entities.nutrition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
public class MealItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_id", nullable = false)
    @JsonIgnore
    private Meal meal;

    @ManyToOne(fetch = FetchType.EAGER) // On veut souvent les détails du produit
    @JoinColumn(name = "product_code", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Double quantity; // en grammes ou unité selon le produit
}
