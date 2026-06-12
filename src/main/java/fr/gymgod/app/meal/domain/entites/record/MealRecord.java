package fr.gymgod.app.meal.domain.entites.record;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MealRecord(
        UUID id,
        String type,
        LocalDateTime date,
        List<MealItemRecord> items
) {}
