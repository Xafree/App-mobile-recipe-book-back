package fr.gymgod.app.security.config;

import fr.gymgod.app.security.domain.port.SecurityDataPort;
import fr.gymgod.common.entities.user.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsServiceAdapter implements UserDetailsService {

    private final SecurityDataPort securityDataPort;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = securityDataPort.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // OAuth users have no password — use a placeholder that can never match a real input
        String password = user.getPassword() != null ? user.getPassword() : "{noop}__oauth__";

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(password)
                .roles(user.getRole())
                .disabled(!user.isActive())
                .build();
    }
}
