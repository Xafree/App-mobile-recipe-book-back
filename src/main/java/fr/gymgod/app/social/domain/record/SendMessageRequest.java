package fr.gymgod.app.social.domain.record;

import java.util.Map;

/**
 * Corps de la requête d'envoi d'un message direct.
 *
 * @param messageType "TEXT" (défaut si null) ou "RECIPE" — voir {@link fr.gymgod.common.entities.social.MessageType}.
 * @param metadata charge utile structurée pour les types non-texte
 *                 (ex. {@code {"recipeId": "...", "recipeName": "...", "imageUrl": "..."}} pour RECIPE).
 */
public record SendMessageRequest(String content, String messageType, Map<String, Object> metadata) {}
