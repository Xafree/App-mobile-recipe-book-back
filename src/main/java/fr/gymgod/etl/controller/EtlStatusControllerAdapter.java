package fr.gymgod.etl.controller;

import fr.gymgod.etl.service.EtlRunStats;
import fr.gymgod.etl.service.EtlStatusHolder;
import fr.gymgod.etl.service.OrchestratorProductEtlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/// Diagnostic de l'ETL produits — évite de devoir dépouiller les logs ou
/// requêter PostgreSQL à chaque doute sur l'état de l'import.
@RestController
@RequestMapping("/admin/api/etl")
@RequiredArgsConstructor
public class EtlStatusControllerAdapter {

    private final EtlStatusHolder statusHolder;
    private final OrchestratorProductEtlService etlService;

    /// @return les statistiques de la dernière exécution, ou 204 si l'ETL n'a jamais tourné.
    @GetMapping("/status")
    public ResponseEntity<EtlRunStats> getStatus() {
        EtlRunStats lastRun = statusHolder.getLastRun();
        if (lastRun == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(lastRun);
    }

    /// Relance l'ETL en synchrone — utile pour forcer un re-import après avoir
    /// remplacé le fichier source manuellement.
    @PostMapping("/run")
    public ResponseEntity<EtlRunStats> runNow(@RequestParam(defaultValue = "auto") String filePath) {
        etlService.loadData(filePath);
        return getStatus();
    }
}
