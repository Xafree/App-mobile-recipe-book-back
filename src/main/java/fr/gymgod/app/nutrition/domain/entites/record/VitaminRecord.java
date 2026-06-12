package fr.gymgod.app.nutrition.domain.entites.record;

import java.util.UUID;

public record VitaminRecord(
        UUID id,
        double a100g,
        double d100g,
        double e100g,
        double k100g,
        double c100g,
        double b1100g,
        double b2100g,
        double pp100g,
        double b6100g,
        double b9100g,
        double b12100g
) {
}
