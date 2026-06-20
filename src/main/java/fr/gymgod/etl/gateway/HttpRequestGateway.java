package fr.gymgod.etl.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;

import fr.gymgod.etl.domain.port.FileDownloaderPort;

/**
 * Service that orchestrates file downloads using HttpGateway and FileGateway.
 * Formerly known as HttpRequestGateway, now acting as an adapter to decouple
 * HTTP
 * and File logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HttpRequestGateway implements FileDownloaderPort {

    private final HttpGateway httpGateway;
    private final FileGateway fileGateway;

    /**
     * Downloads a file from the specified URL and saves it to the given location.
     * If the "Last-Modified" HTTP header is available, the method returns the
     * last modification timestamp in ISO-8601 format. Otherwise, it returns the
     * current timestamp as a fallback.
     *
     * @param url             The URL of the file to download.
     * @param destinationPath The local path where the downloaded file will be
     *                        saved.
     * @return The last modification timestamp in ISO-8601 format, or null if the
     *         download fails.
     */
    public String downloadFile(String url, String destinationPath) {
        log.info("[Téléchargement] Démarrage depuis {}", url);
        long startMs = System.currentTimeMillis();

        // Téléchargement vers un fichier temporaire — le fichier de destination
        // (potentiellement valide depuis un import précédent) n'est remplacé
        // qu'une fois le téléchargement intégralement vérifié. Évite de
        // détruire un bon fichier avec une réponse tronquée/erreur serveur.
        String tempPath = destinationPath + ".download";

        try (HttpGateway.HttpResponse response = httpGateway.get(url, 0)) { // 0 = pas de read timeout (fichier volumineux)
            long expectedBytes = response.getContentLength();
            if (expectedBytes >= 0) {
                log.info("[Téléchargement] Taille annoncée par le serveur : {} Mo",
                        String.format("%.1f", expectedBytes / (1024.0 * 1024.0)));
            } else {
                log.warn("[Téléchargement] Pas d'en-tête Content-Length — impossible de vérifier la complétude du download");
            }

            fileGateway.save(response.getInputStream(), tempPath);

            long actualBytes = java.nio.file.Files.size(java.nio.file.Paths.get(tempPath));
            long elapsedS = (System.currentTimeMillis() - startMs) / 1000;
            double sizeMb = actualBytes / (1024.0 * 1024.0);
            log.info("[Téléchargement] Terminé en {} s — {} reçu ({} Go)",
                    elapsedS, tempPath, String.format("%.1f", sizeMb / 1024));

            // Vérification de complétude : un téléchargement tronqué (connexion
            // coupée en cours de route) laisse un fichier plus petit que prévu.
            if (expectedBytes >= 0 && actualBytes != expectedBytes) {
                log.error("[Téléchargement] Fichier incomplet : {} octets reçus sur {} attendus — fichier de destination conservé tel quel",
                        actualBytes, expectedBytes);
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempPath));
                return null;
            }

            // Téléchargement complet validé : on remplace enfin la destination.
            java.nio.file.Files.move(java.nio.file.Paths.get(tempPath), java.nio.file.Paths.get(destinationPath),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("[Téléchargement] Fichier validé et déplacé vers {}", destinationPath);

            long lastModified = response.getLastModified();
            if (lastModified == 0) {
                log.debug("[Téléchargement] Pas d'en-tête Last-Modified dans la réponse.");
                return String.valueOf(System.currentTimeMillis());
            }
            return Instant.ofEpochMilli(lastModified).toString();

        } catch (IOException e) {
            log.error("[Téléchargement] Échec depuis {} : {} — fichier de destination conservé tel quel", url, e.getMessage());
            try {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempPath));
            } catch (IOException ignored) {
                // best effort
            }
            return null;
        }
    }

    /**
     * Downloads an image from the specified URL and saves it locally with the given
     * name.
     *
     * @param name The name to give to the downloaded image file (extension
     *             included).
     * @param url  The URL from which to download the image.
     * @return true if the image was successfully downloaded and saved, false
     *         otherwise.
     */
    public boolean extractImage(String name, String url) {
        if (fileGateway.imageExists(name)) {
            log.debug("Image already exists locally, skipping download: {}", name);
            return true;
        }

        if (fileGateway.isDownloadQuotaExceeded()) {
            log.debug("Image download quota reached, skipping: {}", name);
            return false;
        }

        try (HttpGateway.HttpResponse response = httpGateway.get(url)) {
            fileGateway.saveImage(response.getInputStream(), name);
            log.info("Image successfully downloaded and saved: {} from URL: {}", name, url);
            return true;
        } catch (IOException e) {
            log.error("Error during image extraction for {}: {}", url, e.getMessage());
            return false;
        }
    }
}
