package fr.gymgod.common.domain.nutrition;

import fr.gymgod.common.entities.nutrition.ReferenceFood;
import fr.gymgod.common.entities.nutrition.ReferenceFoodSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReferenceFoodRepository extends JpaRepository<ReferenceFood, java.util.UUID> {

    Optional<ReferenceFood> findBySourceAndSourceCode(ReferenceFoodSource source, String sourceCode);

    /**
     * Meilleure correspondance par similarité de nom (pg_trgm) pour une source
     * donnée, ou {@code Optional.empty()} si aucune entrée ne dépasse le seuil
     * de similarité 0.5.
     */
    @Query(value = "SELECT * FROM reference_foods WHERE source = :source AND similarity(name, :name) > 0.5 "
            + "ORDER BY similarity(name, :name) DESC LIMIT 1", nativeQuery = true)
    Optional<ReferenceFood> findBestMatch(@Param("name") String name, @Param("source") String source);
}
