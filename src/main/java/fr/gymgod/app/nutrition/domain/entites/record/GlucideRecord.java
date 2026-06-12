package fr.gymgod.app.nutrition.domain.entites.record;

import java.util.UUID;

public record GlucideRecord(
        UUID id,
        double sodium100g,
        double salt100g,
        double potassium100g,
        double magnesium100g,
        double calcium100g
) {
}
