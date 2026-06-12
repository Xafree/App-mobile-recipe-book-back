package fr.gymgod.etl.domain.port;

import fr.gymgod.common.entities.nutrition.*;
import fr.gymgod.common.entities.user.UserAccount;
import fr.gymgod.etl.domain.model.AdditiveData;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port used by the domain to fetch reference data (Brands, Categories, etc.).
 * The implementation detail (JPA Repositories, external API, etc.) is hidden
 * from the core business logic.
 */
public interface ReferenceDataPort {

    Map<String, Brand> resolveBrands(Set<String> names);

    Map<String, Categorie> resolveCategories(Set<String> names);

    Map<String, Country> resolveCountries(Set<String> names);

    Map<String, Label> resolveLabels(Set<String> names);

    Map<String, Ingredient> resolveIngredients(Set<String> names);

    Map<String, Trace> resolveTraces(Set<String> names);

    List<String> getCountryMissing();

    List<Ingredient> getOrCreateIngredients(List<String> names);

    List<Additive> getOrCreateAdditives(List<AdditiveData> additives);
}
