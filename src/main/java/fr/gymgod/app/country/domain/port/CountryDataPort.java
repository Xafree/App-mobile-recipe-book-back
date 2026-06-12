package fr.gymgod.app.country.domain.port;

import fr.gymgod.common.entities.nutrition.Country;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CountryDataPort {
    Page<Country> getCountries(Pageable pageable);

    List<Country> getAllCountries();

    Optional<Country> getCountryById(UUID id);

    Optional<Country> getCountryByNameIgnoreCase(String name);

    Country save(Country country);
}
