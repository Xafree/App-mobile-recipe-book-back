package fr.gymgod.etl.service;

import fr.gymgod.common.entities.nutrition.Country;
import fr.gymgod.common.entities.nutrition.*;
import fr.gymgod.common.entities.user.UserAccount;
import fr.gymgod.etl.domain.service.EtlDataParser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ProductMapper {

    public Product mapToProduct(String[] columns, Product existing,
            Map<String, Brand> brandCache,
            Map<String, Categorie> categorieCache,
            Map<String, Country> countryCache,
            Map<String, Label> labelCache,
            Map<String, Ingredient> ingredientCache,
            Map<String, Trace> traceCache,
            String version) {
        Product p = existing != null ? existing : new Product();

        p.setVersion(version);
        p.setCode(EtlDataParser.getValue(columns, 0));
        p.setCreatedTime(EtlDataParser.getValue(columns, 3));
        p.setLastModifiedTime(EtlDataParser.getValue(columns, 5));
        p.setProductName(EtlDataParser.getValue(columns, 10));
        p.setUrl(EtlDataParser.getValue(columns, 1));
        p.setQuantity(quantity(columns));

        p.setBrand(getBrand(columns, 18, brandCache));
        p.setCategorie(getCategories(columns, 21, categorieCache));
        p.setCountry(getCountries(columns, 39, countryCache));
        p.setLabel(getLabels(columns, 29, labelCache));
        p.setImage(images(columns, 82, 84, 86));

        // Save raw text for AI Scheduler
        p.setIngredientsText(EtlDataParser.getValue(columns, 42));
        p.setIngredient(getIngredients(columns, 42, ingredientCache));
        p.setTrace(getTraces(columns, 47, traceCache));

        // Init empty additives by default
        p.setAdditives(new ArrayList<>());

        p.setNutriscore(EtlDataParser.getValueInt(columns, 57));
        p.setNutriscoreGrade(EtlDataParser.getValue(columns, 58));
        p.setNutriment(nutriment(columns, 89, 150, 129, 92, 146, 130, 93));
        p.setGlucide(glucide(columns, 156, 154, 175, 180, 177));
        p.setVitamin(vitamin(columns, 158, 160, 161, 162, 163, 164, 165, 166, 167, 168, 170));
        p.setNutritionDataIncomplete(isNutritionDataIncomplete(columns));

        return p;
    }

    /**
     * Vrai si le groupe macros, minéraux OU vitamines est entièrement vide dans
     * la ligne CSV — sert à déclencher l'enrichissement à la demande via l'API
     * OpenFoodFacts (cf. NutritionEnrichmentService).
     */
    private boolean isNutritionDataIncomplete(String[] columns) {
        boolean nutrimentEmpty = EtlDataParser.allEmpty(columns, 89, 150, 129, 92, 146, 130, 93);
        boolean glucideEmpty = EtlDataParser.allEmpty(columns, 156, 154, 175, 180, 177);
        boolean vitaminEmpty = EtlDataParser.allEmpty(columns, 158, 160, 161, 162, 163, 164, 165, 166, 167, 168, 170);
        return nutrimentEmpty || glucideEmpty || vitaminEmpty;
    }

    private String quantity(String[] columns) {
        String quantity = EtlDataParser.getValue(columns, 13);
        if (quantity == null)
            return null;
        quantity = quantity.replace(" ", "");
        return quantity.replaceAll("(\\d)(?=[a-zA-Z%])", "$1 ");
    }

    private Brand getBrand(String[] columns, int idxBrand, Map<String, Brand> batchCache) {
        String name = EtlDataParser.getValue(columns, idxBrand);
        if (name == null)
            return null;
        return batchCache.get(name);
    }

    private List<Categorie> getCategories(String[] columns, int idxCategories, Map<String, Categorie> batchCache) {
        List<Categorie> lst = new ArrayList<>();
        String rawCategories = EtlDataParser.getValue(columns, idxCategories);
        if (rawCategories == null || rawCategories.isEmpty()) {
            return lst;
        }

        List<String> stringList = EtlDataParser.splitIgnoringParentheses(rawCategories);
        for (String name : stringList) {
            name = name.trim();
            if (name.isEmpty())
                continue;
            Categorie c = batchCache.get(name);
            if (c != null) {
                lst.add(c);
            }
        }
        return lst;
    }

    private List<Country> getCountries(String[] columns, int idxCountries, Map<String, Country> batchCache) {
        List<Country> lst = new ArrayList<>();
        String rawCountries = EtlDataParser.getValue(columns, idxCountries);
        if (rawCountries == null || rawCountries.isEmpty()) {
            return lst;
        }

        List<String> stringList = EtlDataParser.splitIgnoringParentheses(rawCountries);
        for (String name : stringList) {
            name = name.trim();
            if (name.isEmpty())
                continue;
            Country c = batchCache.get(name);
            if (c != null) {
                lst.add(c);
            }
        }
        return lst;
    }

    private List<Label> getLabels(String[] columns, int idxLabels, Map<String, Label> batchCache) {
        List<Label> lst = new ArrayList<>();
        String rawLabels = EtlDataParser.getValue(columns, idxLabels);
        if (rawLabels == null || rawLabels.isEmpty()) {
            return lst;
        }

        List<String> stringList = EtlDataParser.splitIgnoringParentheses(rawLabels);
        for (String name : stringList) {
            name = name.trim();
            if (name.isEmpty())
                continue;
            Label l = batchCache.get(name);
            if (l != null) {
                lst.add(l);
            }
        }
        return lst;
    }

    private Images images(String[] columns, int idxImageUrl, int idxImageIngredientUrl, int idxImageNutritionUrl) {
        String imageUrl = EtlDataParser.getValue(columns, idxImageUrl);
        String imageIngredientUrl = EtlDataParser.getValue(columns, idxImageIngredientUrl);
        String imageNutritionUrl = EtlDataParser.getValue(columns, idxImageNutritionUrl);
        Images images = new Images();

        if (imageUrl != null && !imageUrl.contains("/invalid")) {
            images.setImageUrl(imageUrl);
        }
        if (imageIngredientUrl != null && !imageIngredientUrl.contains("/invalid")) {
            images.setImageIngredientUrl(imageIngredientUrl);
        }
        if (imageNutritionUrl != null && !imageNutritionUrl.contains("/invalid")) {
            images.setImageNutritionUrl(imageNutritionUrl);
        }

        return images;
    }

    private List<Ingredient> getIngredients(String[] columns, int idxIngredients, Map<String, Ingredient> batchCache) {
        List<Ingredient> lst = new ArrayList<>();
        String rawIngredients = EtlDataParser.getValue(columns, idxIngredients);
        if (rawIngredients == null || rawIngredients.isEmpty()) {
            return lst;
        }

        List<String> stringList = EtlDataParser.splitIgnoringParentheses(rawIngredients);
        for (String name : stringList) {
            name = name.trim();
            if (name.isEmpty())
                continue;
            Ingredient c = batchCache.get(name);
            if (c != null) {
                lst.add(c);
            }
        }
        return lst;
    }

    private List<Trace> getTraces(String[] columns, int idxTraces, Map<String, Trace> batchCache) {
        List<Trace> lst = new ArrayList<>();
        String rawTraces = EtlDataParser.getValue(columns, idxTraces);
        if (rawTraces == null || rawTraces.isEmpty()) {
            return lst;
        }

        List<String> stringList = EtlDataParser.splitIgnoringParentheses(rawTraces);
        for (String name : stringList) {
            name = name.trim();
            if (name.isEmpty())
                continue;
            Trace c = batchCache.get(name);
            if (c != null) {
                lst.add(c);
            }
        }
        return lst;
    }

    private Nutriment nutriment(String[] columns, int idxEnergyKcal100g,
            int idxProteins100g,
            int idxCarbohydrates100g,
            int idxFat100g,
            int idxFiber100g,
            int idxSugars100g,
            int idxSaturatedFat100g) {
        Nutriment nutriment = new Nutriment();
        nutriment.setEnergyKcal100g(EtlDataParser.getValueDouble(columns, idxEnergyKcal100g));
        nutriment.setProteins100g(EtlDataParser.getValueDouble(columns, idxProteins100g));
        nutriment.setCarbohydrates100g(EtlDataParser.getValueDouble(columns, idxCarbohydrates100g));
        nutriment.setFat100g(EtlDataParser.getValueDouble(columns, idxFat100g));
        nutriment.setFiber100g(EtlDataParser.getValueDouble(columns, idxFiber100g));
        nutriment.setSugars100g(EtlDataParser.getValueDouble(columns, idxSugars100g));
        nutriment.setSaturatedFat100g(EtlDataParser.getValueDouble(columns, idxSaturatedFat100g));

        return nutriment;
    }

    private Glucide glucide(String[] columns, int idxSodium100g, int idxSalt100g, int idxPotassium100g,
            int idxMagnesium100g, int idxCalcium100g) {
        Glucide glucide = new Glucide();
        glucide.setSodium100g(EtlDataParser.getValueDouble(columns, idxSodium100g));
        glucide.setSalt100g(EtlDataParser.getValueDouble(columns, idxSalt100g));
        glucide.setPotassium100g(EtlDataParser.getValueDouble(columns, idxPotassium100g));
        glucide.setMagnesium100g(EtlDataParser.getValueDouble(columns, idxMagnesium100g));
        glucide.setCalcium100g(EtlDataParser.getValueDouble(columns, idxCalcium100g));

        return glucide;
    }

    private Vitamin vitamin(String[] columns, int idxA100g, int idxD100g, int idxE100g, int idxK100g, int idxC100g,
            int idxB1100g, int idxB2100g, int idxPp100g, int idxB6100g, int idxB9100g, int idxB12100g) {
        Vitamin vitamin = new Vitamin();
        vitamin.setA100g(EtlDataParser.getValueDouble(columns, idxA100g));
        vitamin.setD100g(EtlDataParser.getValueDouble(columns, idxD100g));
        vitamin.setE100g(EtlDataParser.getValueDouble(columns, idxE100g));
        vitamin.setK100g(EtlDataParser.getValueDouble(columns, idxK100g));
        vitamin.setC100g(EtlDataParser.getValueDouble(columns, idxC100g));
        vitamin.setB1100g(EtlDataParser.getValueDouble(columns, idxB1100g));
        vitamin.setB2100g(EtlDataParser.getValueDouble(columns, idxB2100g));
        vitamin.setPp100g(EtlDataParser.getValueDouble(columns, idxPp100g));
        vitamin.setB6100g(EtlDataParser.getValueDouble(columns, idxB6100g));
        vitamin.setB9100g(EtlDataParser.getValueDouble(columns, idxB9100g));
        vitamin.setB12100g(EtlDataParser.getValueDouble(columns, idxB12100g));

        return vitamin;
    }
}
