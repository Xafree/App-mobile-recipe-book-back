package fr.gymgod.app.security.domain.port;

import fr.gymgod.common.entities.user.UserAccount;

import java.util.Optional;

public interface SecurityDataPort {
    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByGoogleId(String googleId);

    Optional<UserAccount> findByAppleId(String appleId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    UserAccount save(UserAccount user);
}
