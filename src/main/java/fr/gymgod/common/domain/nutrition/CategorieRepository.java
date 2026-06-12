package fr.gymgod.common.domain.nutrition;

import fr.gymgod.common.entities.nutrition.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategorieRepository extends JpaRepository<Categorie, UUID> {
    Optional<Categorie> findByName(String name);
    List<Categorie> findByNameIn(Collection<String> names);
}
