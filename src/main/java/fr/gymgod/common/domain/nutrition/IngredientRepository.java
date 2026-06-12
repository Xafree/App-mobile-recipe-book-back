package fr.gymgod.common.domain.nutrition;

import fr.gymgod.common.entities.nutrition.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {
    Optional<Ingredient> findByName(String name);
    List<Ingredient> findByNameIgnoreCase(String name);
    List<Ingredient> findByNameIn(Collection<String> names);
}
