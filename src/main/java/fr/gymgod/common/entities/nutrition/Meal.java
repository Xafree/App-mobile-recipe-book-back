package fr.gymgod.common.entities.nutrition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String type; // breakfast, lunch, dinner, snack

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MealItem> items = new ArrayList<>();

    // Méthodes utilitaires pour la cohérence de la relation bidirectionnelle
    public void addItem(MealItem item) {
        items.add(item);
        item.setMeal(this);
    }

    public void removeItem(MealItem item) {
        items.remove(item);
        item.setMeal(null);
    }
}
