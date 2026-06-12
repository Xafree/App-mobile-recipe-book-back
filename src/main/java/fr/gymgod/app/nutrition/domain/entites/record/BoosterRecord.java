package fr.gymgod.app.nutrition.domain.entites.record;

import java.util.UUID;

public record BoosterRecord(
        UUID id,
        double iron100g,
        double caffeine100g,
        double taurine100g,
        double carnitine100g,
        double nutritionScoreFr100g,
        double alcohol100g
) {
}
