package fr.gymgod.common.image;

import org.springframework.http.MediaType;

/**
 * Résout le {@link MediaType} d'une image servie depuis le disque, à partir
 * de son extension de fichier — évite de servir des images en
 * {@code application/octet-stream}, ce qui dégrade le décodage/cache côté
 * client.
 */
public final class ImageContentTypes {

    private ImageContentTypes() {
    }

    public static MediaType resolve(String filename) {
        String ext = filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase()
                : "";
        return switch (ext) {
            case "png" -> MediaType.IMAGE_PNG;
            case "webp" -> MediaType.valueOf("image/webp");
            case "gif" -> MediaType.IMAGE_GIF;
            default -> MediaType.IMAGE_JPEG;
        };
    }
}
