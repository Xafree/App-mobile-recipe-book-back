package fr.gymgod.app.ingredient.gateway;

import fr.gymgod.app.ingredient.domain.port.CatalogIngredientDataPort;
import fr.gymgod.common.domain.nutrition.CatalogIngredientRepository;
import fr.gymgod.common.entities.nutrition.CatalogIngredient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CatalogIngredientGateway implements CatalogIngredientDataPort {

    private final CatalogIngredientRepository catalogIngredientRepository;

    @Override
    public Optional<CatalogIngredient> findByName(String name) {
        return catalogIngredientRepository.findByNameIgnoreCase(name.trim());
    }

    @Override
    public Optional<CatalogIngredient> findByExternalFoodCode(String externalFoodCode) {
        return catalogIngredientRepository.findByExternalFoodCode(externalFoodCode);
    }

    @Override
    public List<CatalogIngredient> search(String name, int limit) {
        return catalogIngredientRepository.findByNameContainingIgnoreCaseOrderByNameAsc(
                name.trim(), PageRequest.of(0, limit));
    }

    @Override
    public CatalogIngredient save(CatalogIngredient ingredient) {
        return catalogIngredientRepository.save(ingredient);
    }
}
