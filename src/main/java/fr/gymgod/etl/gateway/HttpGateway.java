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

    public static class HttpResponse implements AutoCloseable {
        private final HttpURLConnection connection;
        @Getter
        private final InputStream inputStream;
        @Getter
        private final long lastModified;

        public HttpResponse(HttpURLConnection connection) throws IOException {
            this.connection = connection;
            this.inputStream = connection.getInputStream();
            this.lastModified = connection.getLastModified();
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
     * @throws IOException If the connection fails or the response code is not 200
     *                     OK.
     */
    public HttpResponse get(String urlString) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
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
