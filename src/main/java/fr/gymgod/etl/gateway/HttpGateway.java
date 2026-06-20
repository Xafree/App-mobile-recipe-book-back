package fr.gymgod.etl.gateway;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

@Service
@Slf4j
public class HttpGateway {

    private static final int CONNECT_TIMEOUT = 5000; // 5 seconds
    private static final int READ_TIMEOUT = 10000; // 10 seconds
    // Timeout dédié au téléchargement de fichiers volumineux (ex : dump OFF ~10 Go)
    private static final int LARGE_FILE_READ_TIMEOUT = 0; // 0 = pas de timeout

    public static class HttpResponse implements AutoCloseable {
        private final HttpURLConnection connection;
        @Getter
        private final InputStream inputStream;
        @Getter
        private final long lastModified;
        // -1 si le serveur n'envoie pas l'en-tête Content-Length (ex : réponse chunked)
        @Getter
        private final long contentLength;

        public HttpResponse(HttpURLConnection connection) throws IOException {
            this.connection = connection;
            this.inputStream = connection.getInputStream();
            this.lastModified = connection.getLastModified();
            this.contentLength = connection.getContentLengthLong();
        }

        @Override
        public void close() {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Executes an HTTP GET request and returns the response.
     *
     * @param urlString The target URL.
     * @return HttpResponse containing the InputStream and Last-Modified time.
     * @throws IOException If the connection fails or the response code is not 200 OK.
     */
    public HttpResponse get(String urlString) throws IOException {
        return get(urlString, READ_TIMEOUT);
    }

    /**
     * Executes an HTTP GET request avec un read timeout personnalisé.
     * Utiliser {@code readTimeoutMs = 0} pour les téléchargements volumineux
     * (ex : dump OpenFoodFacts) afin d'éviter une coupure prématurée.
     *
     * @param urlString    The target URL.
     * @param readTimeoutMs Read timeout en ms ; 0 = pas de timeout.
     * @return HttpResponse containing the InputStream and Last-Modified time.
     * @throws IOException If the connection fails or the response code is not 200 OK.
     */
    public HttpResponse get(String urlString, int readTimeoutMs) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(readTimeoutMs);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible)");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return new HttpResponse(connection);
        } else {
            connection.disconnect();
            throw new IOException("HTTP GET failed with code " + responseCode);
        }
    }
}
