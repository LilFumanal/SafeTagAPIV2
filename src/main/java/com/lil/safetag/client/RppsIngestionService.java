package com.lil.safetag.client;

import com.lil.safetag.entity.PracticeLocation;
import com.lil.safetag.entity.RppsPractitioner;
import com.lil.safetag.repository.RppsPractitionerRepository;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class RppsIngestionService {

    // Constantes métier
    private static final List<String> ALLOWED_ROLES = List.of("10", "93");
    private static final List<String> ALLOWED_SPECIALTIES = List.of("SM33", "SM42", "SM43", "SM92", "SM93");

    // Index des colonnes
    private static final int COL_ID_PP = 1;          // Identifiant PP (RPPS)
    private static final int COL_LASTNAME = 3;       // Nom d'exercice
    private static final int COL_FIRSTNAME = 4;      // Prénom d'exercice
    private static final int COL_PROFESSION_CODE = 5; // Code profession (ex: 10, 93)
    private static final int COL_SPECIALTY_CODE = 11; // Code savoir-faire (Spécialité)

    private static final int BATCH_SIZE = 1000;
    private final RppsPractitionerRepository repository;
    private final CSVClient csvClient;

    public RppsIngestionService(RppsPractitionerRepository repository, CSVClient csvClient) {
        this.repository = repository;
        this.csvClient = csvClient;
    }

    public void importRppsData() {
        log.info("[START] Début de l'importation dans PostgreSQL...");
        List<RppsPractitioner> batch = new ArrayList<>();
        Set<String> processedIds = new HashSet<>();
        int totalSaved = 0;

        try (InputStream rawStream = csvClient.downloadRppsFile();
             CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(rawStream, StandardCharsets.UTF_8))
                     .withCSVParser(new CSVParserBuilder().withSeparator('|').build())
                     .withSkipLines(1) // Saute l'en-tête automatiquement
                     .build()) {

            String[] row;
            int lineCount = 0;
            while ((row = csvReader.readNext()) != null) {
                if (lineCount >= 100) {
                    System.out.println("[DEBUG] 20 lignes atteintes, arrêt de la lecture.");
                    break;
                }
                String street = String.format("%s %s %s %s",
                        getValueOrEmpty(row, 28),
                        getValueOrEmpty(row, 29),
                        getValueOrEmpty(row, 31),
                        getValueOrEmpty(row, 32)
                ).replaceAll("\\s+", " ").trim(); // Nettoie les espaces en trop si une colonne est vide

                PracticeLocation location = new PracticeLocation();
                location.setFacilityName(getValueOrEmpty(row, 24));
                location.setStreet(street.isEmpty() ? null : street);
                location.setZipCode(getValueOrEmpty(row, 35));
                location.setCity(getValueOrEmpty(row, 37));

                if (isValid(row, processedIds)) {
                    batch.add(mapToEntity(row,location ));
                    processedIds.add(row[COL_ID_PP]); // On le marque comme "vu"
                }

                if (batch.size() >= BATCH_SIZE) {
                    repository.saveAll(batch);
                    totalSaved += batch.size();
                    log.info("Batch sauvegardé. Total : {}", totalSaved);
                    batch.clear();
                }
                lineCount++;

                }
            // Enregistrer le reliquat
            if (!batch.isEmpty()) {
                repository.saveAll(batch);
                totalSaved += batch.size();
            }


            log.info("[SUCCESS] Importation terminée. {} praticiens en base.", totalSaved);

        } catch (Exception e) {
            log.error("Erreur critique lors de l'importation", e);
            throw new RuntimeException("L'importation a échoué : " + e.getMessage());
        }
    }

    private RppsPractitioner mapToEntity(String[] row, PracticeLocation location) {
        RppsPractitioner p = new RppsPractitioner();
        p.setRppsId(row[COL_ID_PP]);
        p.setLastName(row[COL_LASTNAME]);
        p.setFirstName(row[COL_FIRSTNAME]);
        p.setProfessionCode(row[COL_PROFESSION_CODE]);
        p.setSpecialtyCode(row[COL_SPECIALTY_CODE]);
        p.setLastUpdated(LocalDateTime.now());
        p.addLocation(location);
        return p;
    }

    private boolean isValid(String[] row, Set<String> processedIds) {
        // 1. Contrôle structurel de la ligne
        if (row == null || row.length < 13) {
            return false;
        }

        // 2. Contrôle anti-doublon
        String rppsId = row[COL_ID_PP];
        if (processedIds.contains(rppsId)) {
            return false;
        }

        // 3. Contrôle métier (Rôle & Spécialité)
        String professionCode = row[COL_PROFESSION_CODE];

        if (!ALLOWED_ROLES.contains(professionCode)) {
            return false;
        }

        if ("93".equals(professionCode)) {
            return true; // Les psychologues sont acceptés directement
        }

        if ("10".equals(professionCode)) {
            String specialtyCode = row[COL_SPECIALTY_CODE];
            return ALLOWED_SPECIALTIES.contains(specialtyCode); // Les médecins nécessitent une spécialité précise
        }

        return false;
    }
    private String getValueOrEmpty(String[] row, int index) {
        return (row.length > index && row[index] != null) ? row[index].trim() : "";
    }
}
