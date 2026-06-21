package fr.gymgod.app.security.controller.dto;

import fr.gymgod.common.entities.user.UserAccount;

import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String username,
        String firstName,
        String lastName,
        String avatarUrl,
        String bio,
        Integer calorieGoal,
        Boolean notificationsEnabled,
        String language,
        String provider,
        /** Email vérifié — le client bloque l'accès à l'app tant que false (cf. écran de vérification). */
        boolean emailVerified
) {
    public static UserDto from(UserAccount user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getCalorieGoal(),
                user.getNotificationsEnabled(),
                user.getLanguage(),
                user.getProvider() != null ? user.getProvider().name().toLowerCase() : "email",
                user.isEmailVerified()
        );
    }
}
