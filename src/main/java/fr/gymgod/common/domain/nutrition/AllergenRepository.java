package fr.gymgod.common.domain.nutrition;

import fr.gymgod.common.entities.nutrition.Allergen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AllergenRepository extends JpaRepository<Allergen, UUID> {
    Optional<Allergen> findByName(String name);
    List<Allergen> findByNameIn(Collection<String> names);
}
