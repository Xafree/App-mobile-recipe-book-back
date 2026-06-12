package fr.gymgod.etl.domain.port;

/**
 * Port used by the domain to fetch an external file/image.
 * The implementation detail (HTTP vs FTP, etc.) is hidden from the core
 * business logic.
 */
public interface FileDownloaderPort {

    /**
     * Downloads a file from the specified URL and saves it to the given location.
     *
     * @param url             The URL of the file to download.
     * @param destinationPath The local path where the downloaded file will be
     *                        saved.
     * @return Version string (such as last modified date)
     */
    String downloadFile(String url, String destinationPath);

    /**
     * Downloads an image from the specified URL to a standardized target
     * destination.
     *
     * @param name The name to give to the downloaded image file.
     * @param url  The URL from which to download the image.
     * @return true if successful, false otherwise.
     */
    boolean extractImage(String name, String url);
}
