package fr.gymgod.common.domain.nutrition;

import fr.gymgod.common.entities.nutrition.Country;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CountryRepository extends JpaRepository<Country, UUID> {
    Optional<Country> findByNameIgnoreCase(String name);

    List<Country> findByNameIn(Collection<String> names);

    @Query("SELECT c FROM Country c JOIN c.variants v WHERE LOWER(v) = LOWER(:variant)")
    List<Country> findByVariantIgnoreCase(@Param("variant") String variant, Pageable pageable);
}
