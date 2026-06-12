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

@Service
@Slf4j
public class FileGateway {

    @Value("${path.image}")
    private String imageStoragePath;

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
        Files.copy(inputStream, fullPath, StandardCopyOption.REPLACE_EXISTING);
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
