package fr.gymgod.app.country.controller;

import fr.gymgod.app.country.service.OrchestratorCountry;
import fr.gymgod.common.entities.nutrition.Country;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/api/countries")
@RequiredArgsConstructor
@Slf4j
public class CountryControllerAdapter {

    private final OrchestratorCountry orchestratorCountry;

    @GetMapping
    public Page<Country> getAllCountries(Pageable pageable) {
        return orchestratorCountry.getAllCountries(pageable);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Country> updateCountry(@PathVariable UUID id, @RequestBody Country countryUpdate) {
        return orchestratorCountry.updateCountry(id, countryUpdate)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/export")
    public void exportCountries(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"countries.csv\"");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try (PrintWriter writer = response.getWriter()) {
            writer.write('\ufeff');
            writer.println("Canonical Name;Variants (semicolon separated)");

            List<Country> countries = orchestratorCountry.getAllCountriesList();
            for (Country country : countries) {
                String variants = String.join(";", country.getVariants());
                writer.println(escape(country.getName()) + ";" + variants);
            }
        }
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importCountries(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            String resultMessage = orchestratorCountry.importCountries(file);
            return ResponseEntity.ok(resultMessage);
        } catch (IOException e) {
            log.error("Error importing countries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error importing file: " + e.getMessage());
        }
    }

    private String escape(String data) {
        if (data == null)
            return "";
        return data;
    }
}
