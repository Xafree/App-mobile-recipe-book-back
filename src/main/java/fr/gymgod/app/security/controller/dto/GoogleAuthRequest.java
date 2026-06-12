package fr.gymgod.app.security.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(@NotBlank String idToken) {}
