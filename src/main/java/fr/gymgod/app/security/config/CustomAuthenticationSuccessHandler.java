package fr.gymgod.app.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gymgod.app.security.controller.dto.AuthResponse;
import fr.gymgod.app.security.controller.dto.UserDto;
import fr.gymgod.common.entities.user.UserAccount;
import fr.gymgod.app.security.domain.port.SecurityDataPort;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final SecurityDataPort securityDataPort;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        // Récupérer l'utilisateur complet depuis la base
        String username = authentication.getName();
        UserAccount user = securityDataPort.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Stocker les attributs utiles dans la session
        HttpSession session = request.getSession();
        session.setAttribute("USER_ID", user.getId().toString());
        session.setAttribute("USER_EMAIL", user.getEmail());

        // Réponse JSON — même format que les endpoints OAuth (/auth/google, /auth/apple)
        // pour que le client Flutter puisse parser le résultat de façon uniforme
        AuthResponse authResponse = AuthResponse.of(UserDto.from(user));

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), authResponse);
    }
}
