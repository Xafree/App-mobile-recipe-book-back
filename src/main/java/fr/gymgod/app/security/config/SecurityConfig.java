package fr.gymgod.app.security.config;

import fr.gymgod.app.config.RateLimitFilter;
import fr.gymgod.common.constants.ConstantsCommon;
import fr.gymgod.common.constants.LoggingConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String LOGOUT_URL = "/api/logout";
    private static final String LOGIN_URL  = "/api/login";

    private final CustomAuthenticationSuccessHandler successHandler;
    private final RateLimitFilter rateLimitFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF désactivé — API mobile Flutter, pas de navigateur avec session partagée.
            // SameSite=Strict sur le cookie JSESSIONID assure la protection CSRF côté web.
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authorize -> authorize
                // ── Catalogue nutrition — lecture publique (résout le 401 Flutter) ──────
                .requestMatchers(HttpMethod.GET,
                    ConstantsCommon.ENDPOINT_NUTRITION + "/search",
                    ConstantsCommon.ENDPOINT_NUTRITION + "/image/**").permitAll()
                .requestMatchers(HttpMethod.GET,
                    ConstantsCommon.ENDPOINT_NUTRITION + "/{key}").permitAll()
                // ── Admin / maintenance — ADMIN uniquement ────────────────────────────
                .requestMatchers(
                    ConstantsCommon.ENDPOINT_NUTRITION + "/admin/**",
                    ConstantsCommon.ENDPOINT_NUTRITION + "/ingredients/cleanup").hasRole("ADMIN")
                // ── Recettes — images publiques, reste authentifié ────────────────────
                .requestMatchers(HttpMethod.GET, "/api/v1/recipes/image/**").permitAll()
                .requestMatchers("/api/v1/recipes/**").authenticated()
                // ── Catalogue d'ingrédients partagé — authentifié uniquement ──────────
                .requestMatchers("/api/v1/ingredients/**").authenticated()
                // ── Auth + repas + utilisateur ────────────────────────────────────────
                .requestMatchers(LOGIN_URL, "/api/auth/**", "/api/user",
                    "/nutrition/meals/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // ── Social (follow + messagerie) — authentifié ────────────────────────
                .requestMatchers("/api/v1/social/**").authenticated()
                // ── Flux SSE notifications temps réel — authentifié ───────────────────
                .requestMatchers("/api/v1/stream").authenticated()
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginProcessingUrl(LOGIN_URL)
                .successHandler(successHandler)
                .failureHandler(jsonAuthenticationFailureHandler())
                .permitAll()
            )
            .exceptionHandling(e -> e
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .logout(logout -> logout
                .logoutUrl(LOGOUT_URL)
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK))
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // Rate limiting avant la vérification d'auth Spring Security
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            // Capture du userId pour le logging (Spring Security vide le SecurityContext ensuite)
            .addFilterAfter(new OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest req,
                                                HttpServletResponse resp,
                                                FilterChain chain) throws ServletException, IOException {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
                        req.setAttribute(LoggingConstants.REQUEST_ATTR_USER_ID, auth.getName());
                    }
                    chain.doFilter(req, resp);
                }
            }, AuthorizationFilter.class);

        return http.build();
    }

    private AuthenticationFailureHandler jsonAuthenticationFailureHandler() {
        return (request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"invalid_credentials\",\"message\":\"Identifiants incorrects\"}");
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Angular frontend de l'admin (localhost:4200) + éventuels outils web locaux
        configuration.setAllowedOriginPatterns(List.of("http://localhost:4200", "http://localhost:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
