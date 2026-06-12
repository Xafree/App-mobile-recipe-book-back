package fr.gymgod.etl.gateway.llm;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LlmRequest {
    private String model;
    private String prompt;
    private boolean stream;
    private String format;
    private java.util.Map<String, Object> options;
}
