package fr.gymgod.app.security.gateway;

import fr.gymgod.app.security.domain.port.SecurityDataPort;
import fr.gymgod.common.domain.user.UserAccountRepository;
import fr.gymgod.common.entities.user.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SecurityGateway implements SecurityDataPort {
    private final UserAccountRepository userAccountRepository;

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return userAccountRepository.findByUsername(username);
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return userAccountRepository.findByEmail(email);
    }

    @Override
    public Optional<UserAccount> findByGoogleId(String googleId) {
        return userAccountRepository.findByGoogleId(googleId);
    }

    @Override
    public Optional<UserAccount> findByAppleId(String appleId) {
        return userAccountRepository.findByAppleId(appleId);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userAccountRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userAccountRepository.existsByEmail(email);
    }

    @Override
    public UserAccount save(UserAccount user) {
        return userAccountRepository.save(user);
    }
}
