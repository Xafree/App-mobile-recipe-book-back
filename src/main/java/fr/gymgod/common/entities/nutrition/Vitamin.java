package fr.gymgod.common.entities.nutrition;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "vitamins")
@Data
public class Vitamin {
    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    // vitamin-a_100g
    private double a100g;
    // vitamin-d_100g
    private double d100g;
    // vitamin-e_100g
    private double e100g;
    // vitamin-k_100g
    private double k100g;
    // vitamin-c_100g
    private double c100g;
    // vitamin-b1_100g
    private double b1100g;
    // vitamin-b2_100g
    private double b2100g;
    // vitamin-pp_100g
    private double pp100g;
    // vitamin-b6_100g
    private double b6100g;
    // vitamin-b9_100g
    private double b9100g;
    // vitamin-b12_100g
    private double b12100g;
}
