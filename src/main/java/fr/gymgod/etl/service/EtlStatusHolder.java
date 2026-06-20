package fr.gymgod.etl.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/// Garde en mémoire le résultat de la dernière exécution de l'ETL produits —
/// {@code null} si l'ETL n'a jamais tourné depuis le démarrage de l'application.
@Component
public class EtlStatusHolder {

    private final AtomicReference<EtlRunStats> lastRun = new AtomicReference<>();

    public void record(EtlRunStats stats) {
        lastRun.set(stats);
    }

    public EtlRunStats getLastRun() {
        return lastRun.get();
    }
}
