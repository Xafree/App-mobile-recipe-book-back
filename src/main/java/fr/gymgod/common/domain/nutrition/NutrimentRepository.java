package fr.gymgod.common.domain.nutrition;

import fr.gymgod.common.entities.nutrition.Nutriment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NutrimentRepository extends JpaRepository<Nutriment, UUID> {
}
