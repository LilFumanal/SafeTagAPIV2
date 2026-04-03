package com.lil.safetag.controller;

import com.lil.safetag.client.RppsIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rpps")
@RequiredArgsConstructor
public class RppsController {

    private final RppsIngestionService ingestionService;

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
}