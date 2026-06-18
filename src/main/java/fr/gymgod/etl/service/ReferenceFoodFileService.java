package fr.gymgod.etl.service;

import fr.gymgod.etl.domain.port.FileDownloaderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Télécharge un fichier de référentiel (CIQUAL, USDA) si le fichier local est
 * absent ou date de plus de 7 jours, sinon réutilise le fichier local — même
 * politique de rafraîchissement que {@link EtlFileService} pour le CSV OFF.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReferenceFoodFileService {

    private final FileDownloaderPort fileDownloaderPort;

    public String ensureFileAvailable(String url, String localPath) {
        Path path = Paths.get(localPath);

        try {
            if (Files.exists(path)) {
                FileTime lastModifiedTime = Files.getLastModifiedTime(path);
                Instant lastModified = lastModifiedTime.toInstant();
                Instant oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS);

                if (lastModified.isAfter(oneWeekAgo)) {
                    log.info("Recent local file {} (less than 7 days). Download ignored.", localPath);
                    return localPath;
                }
            }
        } catch (Exception e) {
            log.warn("Error checking local file {}", localPath, e);
        }

        log.info("Downloading reference data file from {} to {}", url, localPath);
        String version = fileDownloaderPort.downloadFile(url, localPath);
        if (version == null) {
            log.error("Failed to download reference data file from {}", url);
            return Files.exists(path) ? localPath : null;
        }
        return localPath;
    }
}
