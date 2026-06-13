package fr.gymgod.common.constants;

public final class RecipeConstants {

    public static final int    MAX_TITLE_LENGTH                    = 150;
    public static final int    MAX_CATEGORY_LENGTH                 = 50;
    public static final int    MIN_SERVINGS                        = 1;
    public static final int    MAX_SERVINGS                        = 12;
    public static final String MAX_CALORIES_PER_100G               = "900.0";
    public static final String MAX_MACRO_PER_100G                  = "100.0";
    public static final String MAX_SODIUM_MG_PER_100G              = "4000.0";
    public static final String UNIT_PATTERN                        = "g|ml|piece";
    public static final int    MAX_EXTERNAL_FOOD_CODE_LENGTH       = 64;
    public static final int    MAX_EXTERNAL_PRODUCT_SNAPSHOT_LENGTH = 32_768;
    public static final int    MACRO_AGGREGATION_SCALE             = 2;
    public static final String PER_HUNDRED_GRAMS                   = "100";

    private RecipeConstants() {}
}
