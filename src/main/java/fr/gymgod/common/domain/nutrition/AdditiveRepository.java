package fr.gymgod.common.domain.nutrition;

import fr.gymgod.common.entities.nutrition.Additive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdditiveRepository extends JpaRepository<Additive, String> {
    Optional<Additive> findByCode(String code);
}
