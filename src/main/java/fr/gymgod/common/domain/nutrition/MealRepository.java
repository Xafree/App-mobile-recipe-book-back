package fr.gymgod.common.domain.nutrition;

import fr.gymgod.common.entities.nutrition.Meal;
import fr.gymgod.common.entities.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MealRepository extends JpaRepository<Meal, UUID> {
    List<Meal> findByUserId(UUID userId);

    List<Meal> findByUserIdAndDateBetween(UUID userId, LocalDateTime start, LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT mi.product FROM MealItem mi JOIN mi.meal m WHERE m.userId = :userId AND LOWER(mi.product.productName) LIKE LOWER(CONCAT('%', :name, '%')) GROUP BY mi.product ORDER BY COUNT(mi) DESC")
    List<fr.gymgod.common.entities.nutrition.Product> findTopProductsByUserIdAndNameLike(UUID userId, String name,
            org.springframework.data.domain.Pageable pageable);
}
