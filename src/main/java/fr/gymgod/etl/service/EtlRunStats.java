package fr.gymgod.etl.service;

import java.time.Instant;

/// Résultat d'une exécution de l'ETL produits — exposé via
/// {@code GET /admin/api/etl/status} pour diagnostiquer sans avoir à
/// dépouiller les logs.
public record EtlRunStats(
        Instant finishedAt,
        boolean success,
        String filePath,
        String fileVersion,
        long durationSeconds,
        int linesRead,
        int created,
        int updated,
        int skipped,
        String errorMessage) {

    public static EtlRunStats failure(String filePath, String errorMessage) {
        return new EtlRunStats(Instant.now(), false, filePath, null, 0, 0, 0, 0, 0, errorMessage);
    }

    public static EtlRunStats success(String filePath, String fileVersion, long durationSeconds,
            int linesRead, int created, int updated, int skipped) {
        return new EtlRunStats(Instant.now(), true, filePath, fileVersion, durationSeconds,
                linesRead, created, updated, skipped, null);
    }
}
