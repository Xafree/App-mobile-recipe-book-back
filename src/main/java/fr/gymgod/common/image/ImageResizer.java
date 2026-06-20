package fr.gymgod.common.image;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.Thumbnails.Builder;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Redimensionne les images uploadées (recettes, avatars) côté serveur afin
 * d'éviter de stocker/servir des photos brutes de plusieurs Mo issues d'un
 * téléphone — sans perte visible : une image déjà plus petite que la cible
 * n'est jamais réencodée, et le réencodage JPEG utilise une qualité de 0.92
 * (visuellement indistinguable de l'original).
 */
@Component
@Slf4j
public class ImageResizer {

    private static final float JPEG_QUALITY = 0.92f;

    /**
     * Redimensionne [input] si l'une de ses dimensions dépasse [maxDimension],
     * en conservant le ratio d'aspect (pas de recadrage). Si l'image est déjà
     * plus petite, ou si son format ne peut pas être décodé par {@link ImageIO}
     * (ex. HEIC), retourne les octets d'origine sans modification.
     */
    public ProcessedImage resize(InputStream input, String originalFilename, int maxDimension) throws IOException {
        byte[] original = input.readAllBytes();
        String originalExt = extensionOf(originalFilename);

        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(original));
        } catch (IOException e) {
            log.warn("Image illisible par ImageIO ({}), conservation du fichier d'origine", originalFilename);
            return new ProcessedImage(original, originalExt);
        }
        if (image == null || (image.getWidth() <= maxDimension && image.getHeight() <= maxDimension)) {
            return new ProcessedImage(original, originalExt);
        }

        boolean isPng = "png".equalsIgnoreCase(originalExt);
        String outputFormat = isPng ? "png" : "jpg";

        Builder<BufferedImage> thumbnail = Thumbnails.of(image)
                .size(maxDimension, maxDimension)
                .outputFormat(outputFormat);
        if (!isPng) {
            thumbnail.outputQuality(JPEG_QUALITY);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        thumbnail.toOutputStream(out);
        return new ProcessedImage(out.toByteArray(), outputFormat);
    }

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
