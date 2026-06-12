package fr.gymgod.app.config;

import fr.gymgod.common.exception.EmailAlreadyUsedException;
import fr.gymgod.common.exception.ExternalProductSnapshotTooLargeException;
import fr.gymgod.common.exception.InvalidCredentialsException;
import fr.gymgod.common.exception.InvalidTokenException;
import fr.gymgod.common.exception.RecipeNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<Map<String, String>> handleEmailAlreadyUsed(EmailAlreadyUsedException ex) {
        return error(HttpStatus.CONFLICT, "email_already_used", "This email address is already registered.");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return error(HttpStatus.UNAUTHORIZED, "invalid_credentials", "Invalid email or password.");
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidToken(InvalidTokenException ex) {
        log.warn("Token verification failed for provider={}: {}", ex.getProvider(), ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, "invalid_token",
                "Identity token verification failed. Please sign in again.");
    }

    @ExceptionHandler(RecipeNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleRecipeNotFound(RecipeNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "recipe_not_found", "Recipe not found.");
    }

    @ExceptionHandler(ExternalProductSnapshotTooLargeException.class)
    public ResponseEntity<Map<String, String>> handleSnapshotTooLarge(ExternalProductSnapshotTooLargeException ex) {
        log.warn("Rejected oversized snapshot: ingredient='{}', length={}",
                ex.getIngredientName(), ex.getSerializedLength());
        return error(HttpStatus.BAD_REQUEST, "external_product_snapshot_too_large",
                "One of the ingredients carries an external product snapshot that is too large.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String combined = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "validation_error", combined);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error",
                "An unexpected error occurred. Please try again later.");
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("error", code, "message", message));
    }
}
