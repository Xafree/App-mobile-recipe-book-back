package fr.gymgod.common.entities.nutrition;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "glucides")
@Data
public class Glucide {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    // sodium_100g
    private double sodium100g;
    // salt_100g
    private double salt100g;
    // potassium_100g
    private double potassium100g;
    // magnesium_100g
    private double magnesium100g;
    // calcium_100g
    private double calcium100g;

}
