package fr.gymgod.common.domain.user;

import fr.gymgod.common.entities.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findByEmail(String email);
    Optional<UserAccount> findByGoogleId(String googleId);
    Optional<UserAccount> findByAppleId(String appleId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
