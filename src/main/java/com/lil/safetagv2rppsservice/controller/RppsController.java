package com.lil.safetagv2rppsservice.controller;

import com.lil.safetagv2rppsservice.service.RppsIngestionService;
import com.lil.safetagv2rppsservice.entity.RppsPractitioner;
import com.lil.safetagv2rppsservice.repository.RppsPractitionerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rpps")
@RequiredArgsConstructor
public class RppsController {

    private final RppsIngestionService ingestionService;
    private final RppsPractitionerRepository repository;

    @PostMapping("/import")
    public ResponseEntity<String> triggerImport() {
        System.out.println("[INFO] Requête reçue : Lancement de l'importation RPPS...");

        try {
            ingestionService.importRppsData();
            System.out.println("[SUCCESS] Importation terminée avec succès.");
            return ResponseEntity.ok("Importation réussie.");
        } catch (Exception e) {
            System.err.println("[ERROR] Erreur durant l'importation : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erreur : " + e.getMessage());
        }
    }
    @GetMapping("/{rppsId}")
    public ResponseEntity<RppsPractitioner> getPractitionerById(@PathVariable String rppsId) {
        return repository.findByRppsId(rppsId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}