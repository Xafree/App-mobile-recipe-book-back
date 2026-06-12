package fr.gymgod.app.security.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record AppleAuthRequest(
        @NotBlank String identityToken,
        String givenName,   // null après la première connexion
        String familyName,
        String email        // null après la première connexion (ou relay address Apple)
) {}
