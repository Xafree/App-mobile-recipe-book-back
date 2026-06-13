package fr.gymgod.etl.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
@Slf4j
public class FileGateway {

    @Value("${path.image}")
    private String imageStoragePath;

    // Quota de téléchargement d'images, en Mo. 0 = illimité (production).
    // En dev, permet d'éviter de saturer le disque avec les ~650k images OFF.
    @Value("${etl.image.download.max-total-mb:0}")
    private long maxTotalMb;

    // -1 = pas encore calculée. Initialisée à la volée par balayage du dossier
    // d'images au premier accès, puis maintenue incrémentalement.
    private final AtomicLong totalBytes = new AtomicLong(-1);
    private final Object totalBytesInitLock = new Object();

    /**
     * Saves an InputStream to the specified destination path.
     *
     * @param inputStream     The InputStream of the file to save.
     * @param destinationPath The local path where the file will be saved.
     * @throws IOException If an I/O error occurs during saving.
     */
    public void save(InputStream inputStream, String destinationPath) throws IOException {
        Path target = Paths.get(destinationPath);
        createParentDirectories(target);
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Saves an image InputStream to the image storage path with the given name.
     *
     * @param inputStream The InputStream of the image to save.
     * @param imageName   The name of the image file.
     * @throws IOException If an I/O error occurs during saving.
     */
    public void saveImage(InputStream inputStream, String imageName) throws IOException {
        Path fullPath = Paths.get(imageStoragePath, imageName);
        createParentDirectories(fullPath);
        long written = Files.copy(inputStream, fullPath, StandardCopyOption.REPLACE_EXISTING);
        if (maxTotalMb > 0) {
            getTotalBytes(); // s'assure que le compteur est initialisé avant l'ajout
            totalBytes.addAndGet(written);
        }
    }

    /**
     * Indique si le quota de téléchargement d'images (etl.image.download.max-total-mb)
     * est atteint. Toujours false si le quota est désactivé (valeur 0, par défaut en
     * production).
     *
     * @return true si le quota est atteint, false sinon.
     */
    public boolean isDownloadQuotaExceeded() {
        if (maxTotalMb <= 0) {
            return false;
        }
        return getTotalBytes() >= maxTotalMb * 1024L * 1024L;
    }

    private long getTotalBytes() {
        long current = totalBytes.get();
        if (current >= 0) {
            return current;
        }
        synchronized (totalBytesInitLock) {
            current = totalBytes.get();
            if (current >= 0) {
                return current;
            }
            long computed = computeDirectorySize(Paths.get(imageStoragePath));
            totalBytes.set(computed);
            return computed;
        }
    }

    private long computeDirectorySize(Path dir) {
        if (!Files.exists(dir)) {
            return 0;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            log.warn("Failed to compute image directory size: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Checks if an image file exists locally.
     *
     * @param imageName The name of the image file.
     * @return true if the image already exists, false otherwise.
     */
    public boolean imageExists(String imageName) {
        Path fullPath = Paths.get(imageStoragePath, imageName);
        return Files.exists(fullPath);
    }

    /**
     * Creates parent directories if necessary.
     *
     * @param path The path of the target file.
     * @throws IOException If an I/O error occurs.
     */
    private void createParentDirectories(Path path) throws IOException {
        Path parentDir = path.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
    }
}
