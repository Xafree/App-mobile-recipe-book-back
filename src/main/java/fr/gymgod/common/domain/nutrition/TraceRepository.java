package fr.gymgod.common.domain.nutrition;

import fr.gymgod.common.entities.nutrition.Trace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TraceRepository extends JpaRepository<Trace, UUID> {
    Optional<Trace> findByName(String name);
    List<Trace> findByNameIn(Collection<String> names);
}
