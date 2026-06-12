package fr.gymgod.common.entities.nutrition;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "boosters")
@Data
public class Booster {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    // iron_100g
    private double iron100g;
    // caffeine_100g
    private double caffeine100g;
    // taurine_100g
    private double taurine100g;
    // carnitine_100g
    private double carnitine100g;
    //nutrition-score-fr_100g
    private double nutritionScoreFr100g;
    //alcohol_100g
    private double alcohol100g;
}
