package fr.gymgod.common.domain.nutrition;

import fr.gymgod.common.entities.nutrition.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrandRepository extends JpaRepository<Brand, UUID> {
    Optional<Brand> findByName(String name);
    List<Brand> findByNameIn(Collection<String> names);
}
