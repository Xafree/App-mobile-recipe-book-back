package fr.gymgod.common.domain.nutrition;

import fr.gymgod.common.entities.nutrition.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabelRepository extends JpaRepository<Label, UUID> {
    Optional<Label> findByName(String name);
    List<Label> findByNameIn(Collection<String> names);
}
