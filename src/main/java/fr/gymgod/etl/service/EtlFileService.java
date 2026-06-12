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
import java.time.temporal.ChronoUnit;

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
                    FileTime lastModifiedTime = Files.getLastModifiedTime(path);
                    Instant lastModified = lastModifiedTime.toInstant();
                    Instant oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS);

                    if (lastModified.isAfter(oneWeekAgo)) {
                        log.info("Recent local file (less than 7 days). Download ignored.");
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
                if (version == null)
                    version = String.valueOf(System.currentTimeMillis());
            }
            log.info("File version detected: {}", version);
        }
        return version;
    }
}
