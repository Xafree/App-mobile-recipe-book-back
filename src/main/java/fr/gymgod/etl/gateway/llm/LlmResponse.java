package fr.gymgod.etl.gateway.llm;

import lombok.Data;

@Data
public class LlmResponse {
    private String model;
    private String created_at;
    private String response;
    private boolean done;
}
