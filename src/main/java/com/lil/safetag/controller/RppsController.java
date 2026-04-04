package com.lil.safetag.controller;

import com.lil.safetag.client.RppsIngestionService;
import com.lil.safetag.entity.RppsPractitioner;
import com.lil.safetag.repository.RppsPractitionerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

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