package fr.gymgod.app.country.gateway;

import fr.gymgod.app.country.domain.port.CountryDataPort;
import fr.gymgod.common.domain.nutrition.CountryRepository;
import fr.gymgod.common.entities.nutrition.Country;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CountryGateway implements CountryDataPort {

    private final CountryRepository countryRepository;

    @Override
    public Page<Country> getCountries(Pageable pageable) {
        return countryRepository.findAll(pageable);
    }

    @Override
    public List<Country> getAllCountries() {
        return countryRepository.findAll();
    }

    @Override
    public Optional<Country> getCountryById(UUID id) {
        return countryRepository.findById(id);
    }

    @Override
    public Optional<Country> getCountryByNameIgnoreCase(String name) {
        return countryRepository.findByNameIgnoreCase(name);
    }

    @Override
    public Country save(Country country) {
        return countryRepository.save(country);
    }
}
