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
        log.info("Downloading file from {} to {}", url, destinationPath);
        try (HttpGateway.HttpResponse response = httpGateway.get(url)) {
            fileGateway.save(response.getInputStream(), destinationPath);

            long lastModified = response.getLastModified();
            if (lastModified == 0) {
                log.debug("No Last-Modified header found in response.");
                return String.valueOf(System.currentTimeMillis()); // Fallback to current time
            }
            log.debug("Last-Modified header found: {}", lastModified);
            return Instant.ofEpochMilli(lastModified).toString();

        } catch (IOException e) {
            log.error("Error downloading file from {}: {}", url, e.getMessage());
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
