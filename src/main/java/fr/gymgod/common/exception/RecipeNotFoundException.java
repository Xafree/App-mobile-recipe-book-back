package fr.gymgod.common.exception;

import java.util.UUID;

public class RecipeNotFoundException extends RuntimeException {
    public RecipeNotFoundException(UUID id) {
        super("Recette introuvable : " + id);
    }
}
