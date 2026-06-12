package fr.gymgod.common.entities.nutrition;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "nutriments")
@Data
public class Nutriment {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    // energy-kcal_100g
    private double energyKcal100g;

    // proteins_100g
    private double proteins100g;

    // carbohydrates_100g
    private double carbohydrates100g;

    // fat_100g
    private double fat100g;

    // fiber_100g
    private double fiber100g;
}
