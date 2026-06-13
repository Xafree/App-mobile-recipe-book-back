package fr.gymgod.etl.gateway.llm;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LlmRequest {
    private String model;
    private String prompt;
    private boolean stream;
    // "json" pour un JSON libre, ou un schéma JSON (Map) pour contraindre la
    // structure de la réponse — Ollama accepte les deux formes pour ce champ.
    private Object format;
    private java.util.Map<String, Object> options;
}
