package fr.gymgod.app.security.controller.dto;

public record AuthResponse(UserDto user) {
    public static AuthResponse of(UserDto userDto) {
        return new AuthResponse(userDto);
    }
}
