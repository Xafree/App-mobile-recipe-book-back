package fr.gymgod.app.recipe.service;

import fr.gymgod.app.recipe.domain.entites.record.RecipeItemCreateRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

import static fr.gymgod.common.constants.RecipeConstants.MACRO_AGGREGATION_SCALE;
import static fr.gymgod.common.constants.RecipeConstants.PER_HUNDRED_GRAMS;

public final class RecipeMacroCalculator {

    private static final BigDecimal HUNDRED_GRAMS  = new BigDecimal(PER_HUNDRED_GRAMS);
    private static final int        CONTRIBUTION_SCALE = 6;

    private RecipeMacroCalculator() {}

    public static AggregatedMacros aggregate(List<RecipeItemCreateRecord> ingredients, int servings) {
        BigDecimal calories = sum(ingredients, r -> r.nutrition().caloriesPer100g(), r -> r.quantity());
        BigDecimal protein  = sum(ingredients, r -> r.nutrition().proteinPer100g(),  r -> r.quantity());
        BigDecimal carbs    = sum(ingredients, r -> r.nutrition().carbsPer100g(),    r -> r.quantity());
        BigDecimal fat      = sum(ingredients, r -> r.nutrition().fatPer100g(),      r -> r.quantity());

        return new AggregatedMacros(
                perServing(calories, servings),
                perServing(protein,  servings),
                perServing(carbs,    servings),
                perServing(fat,      servings));
    }

    private static BigDecimal sum(List<RecipeItemCreateRecord> ingredients,
                                  Function<RecipeItemCreateRecord, BigDecimal> macro,
                                  Function<RecipeItemCreateRecord, BigDecimal> qty) {
        return ingredients.stream()
                .map(r -> macro.apply(r).multiply(qty.apply(r))
                        .divide(HUNDRED_GRAMS, CONTRIBUTION_SCALE, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal perServing(BigDecimal total, int servings) {
        return total.divide(BigDecimal.valueOf(servings), MACRO_AGGREGATION_SCALE, RoundingMode.HALF_UP);
    }

    public record AggregatedMacros(
            BigDecimal caloriesPerServing,
            BigDecimal proteinPerServing,
            BigDecimal carbsPerServing,
            BigDecimal fatPerServing
    ) {}
}
