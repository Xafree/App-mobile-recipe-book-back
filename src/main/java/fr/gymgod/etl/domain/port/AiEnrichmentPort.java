package fr.gymgod.etl.domain.port;

import fr.gymgod.etl.domain.model.AdditiveData;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AiEnrichmentPort {
    CompletableFuture<List<String>> cleanIngredients(String rawIngredients);

    CompletableFuture<List<AdditiveData>> extractAdditives(List<String> ingredients);
}
