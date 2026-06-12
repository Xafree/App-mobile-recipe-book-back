package fr.gymgod.common.domain.nutrition;

import fr.gymgod.common.entities.nutrition.Glucide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GlucideRepository extends JpaRepository<Glucide, UUID> {
}
