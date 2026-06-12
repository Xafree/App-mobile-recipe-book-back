package fr.gymgod.common.config;

import fr.gymgod.common.domain.user.UserAccountRepository;
import fr.gymgod.common.entities.user.UserAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class InitAccountBean {

    @Bean
    public CommandLineRunner initData(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String username = "stephan";
            if (userAccountRepository.findByUsername(username).isEmpty()) {
                log.info("Création du compte de test '{}'...", username);
                UserAccount user = new UserAccount();
                user.setUsername(username);
                user.setPassword(passwordEncoder.encode("stephan"));
                user.setEmail("stephan.parichon@gmail.com");
                user.setActive(true);
                user.setEmailVerified(true);
                user.setRole("USER");
                userAccountRepository.save(user);
                log.info("Compte de test '{}' créé avec succès.", username);
            } else {
                log.info("Le compte de test '{}' existe déjà.", username);
            }
        };
    }
}
