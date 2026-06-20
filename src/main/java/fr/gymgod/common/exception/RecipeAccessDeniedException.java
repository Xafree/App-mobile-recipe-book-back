package fr.gymgod.common.exception;

import java.util.UUID;

public class RecipeAccessDeniedException extends RuntimeException {
    public RecipeAccessDeniedException(UUID id) {
        super("Accès refusé à la recette : " + id);
    }
}
