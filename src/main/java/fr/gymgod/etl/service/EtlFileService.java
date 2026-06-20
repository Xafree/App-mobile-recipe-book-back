package fr.gymgod.etl.service;

import fr.gymgod.etl.domain.port.FileDownloaderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class EtlFileService {

    private final FileDownloaderPort fileDownloaderPort;

    @Value("${path.etl.url}")
    private String etlUrl;

    @Value("${path.etl.file}")
    private String etlFile;

    public String getEtlFilePath() {
        return etlFile;
    }

    public String ensureFileAvailableAndGetVersion(String requestedFilePath) {
        String version = "Manual " + java.time.LocalDate.now();
        if (requestedFilePath == null || requestedFilePath.isEmpty() || requestedFilePath.equals("auto")) {
            boolean needDownload = true;

            try {
                Path path = Paths.get(etlFile);
                if (Files.exists(path)) {
                    if (!isValidCsvFile(path)) {
                        log.warn("[Fichier] Fichier local invalide (ne commence pas par 'code\\t') — re-téléchargement forcé");
                    } else {
                        FileTime lastModifiedTime = Files.getLastModifiedTime(path);
                        Instant lastModified = lastModifiedTime.toInstant();
                        long sizeBytes = Files.size(path);
                        double sizeMb = sizeBytes / (1024.0 * 1024.0);
                        log.info("[Fichier] Fichier local valide, téléchargement ignoré — {} ({}) — version {}",
                                etlFile,
                                sizeMb >= 1024 ? String.format("%.1f Go", sizeMb / 1024) : String.format("%.1f Mo", sizeMb),
                                lastModified);
                        needDownload = false;
                        version = lastModified.toString();
                    }
                }
            } catch (Exception e) {
                log.warn("Error checking local file", e);
            }

            if (needDownload) {
                log.info("Automatic download from {}", etlUrl);
                version = fileDownloaderPort.downloadFile(etlUrl, etlFile);
                if (version == null) {
                    version = String.valueOf(System.currentTimeMillis());
                } else {
                    Path downloaded = Paths.get(etlFile);
                    if (!isValidCsvFile(downloaded)) {
                        log.error("[Fichier] Le fichier téléchargé n'est pas un CSV OFF valide — " +
                                "le serveur OFF a peut-être renvoyé une erreur HTTP (rate-limit ?). " +
                                "Vérifiez le contenu de {} et téléchargez manuellement si besoin : https://world.openfoodfacts.org/data",
                                etlFile);
                    }
                }
            }
            log.info("File version detected: {}", version);
        }
        return version;
    }

    /// Vérifie que le fichier commence bien par l'en-tête TSV OFF ("code\t").
    /// Permet de détecter les pages d'erreur HTML/texte renvoyées à la place du dump.
    private boolean isValidCsvFile(Path path) {
        try (java.io.BufferedReader reader = Files.newBufferedReader(path)) {
            String firstLine = reader.readLine();
            return firstLine != null && firstLine.startsWith("code\t");
        } catch (Exception e) {
            log.warn("[Fichier] Impossible de lire l'en-tête de {}", path);
            return false;
        }
    }
}
