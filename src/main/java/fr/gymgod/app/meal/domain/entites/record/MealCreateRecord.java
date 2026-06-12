package fr.gymgod.app.meal.domain.entites.record;

import java.time.LocalDateTime;
import java.util.List;

public record MealCreateRecord(
                String type,
                LocalDateTime date,
                List<MealItemCreateRecord> items,
                java.util.UUID recipeId) {
}
