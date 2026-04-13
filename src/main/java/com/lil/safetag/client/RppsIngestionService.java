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

    // Constantes métier - CODES PROFESSION SANTÉ MENTALE & ADDICTOLOGIE
    private static final List<String> ALLOWED_ROLES = List.of("10", "93", "94", "95");

    // CODES SPÉCIALITÉ/SAVOIR-FAIRE (préfixe SM)
    private static final List<String> ALLOWED_SPECIALTIES = List.of(
            "SM04", "SM54", "SM33", "SM93", "SM26", "SM53", "SM70", "SM65"
    );

    // Index de base
    private static final int COL_ID_PP = 1;           // Identifiant PP
    private static final int COL_LASTNAME = 7;        // Nom d'exercice
    private static final int COL_FIRSTNAME = 8;       // Prénom d'exercice
    private static final int COL_PROFESSION_CODE = 9; // Code profession
    private static final int COL_SPECIALTY_CODE = 15; // Code savoir-faire

    // Index d'adresse
    private static final int COL_FACILITY_NAME = 24;  // Raison sociale (structure)
    private static final int COL_STREET_NUM = 28;     // Numéro Voie
    private static final int COL_STREET_REP = 29;     // Indice de répétition (bis, ter...)
    private static final int COL_STREET_TYPE = 31;    // Libellé type de voie
    private static final int COL_STREET_NAME = 32;    // Libellé Voie
    private static final int COL_ZIP_CODE = 35;       // Code postal
    private static final int COL_CITY = 37;           // Bureau distributeur (Ville)

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
                // BLOC DE TEST : Arrêt après 100 lignes
                if (lineCount >= 100) {
                    log.info("[DEBUG] 100 lignes atteintes, arrêt de la lecture.");
                    break;
                }
                lineCount++;

                // 1. On ignore immédiatement les lignes invalides ou les doublons
                if (!isValid(row, processedIds)) {
                    continue;
                }

                // 2. Formatage des données (uniquement pour les lignes valides)
                String street = String.format("%s %s %s",
                        getValueOrEmpty(row, COL_STREET_REP),
                        getValueOrEmpty(row, COL_STREET_TYPE),
                        getValueOrEmpty(row, COL_STREET_NAME)
                ).replaceAll("\\s+", " ").trim();

                PracticeLocation location = new PracticeLocation();
                location.setStreetNumber(getValueOrEmpty(row, COL_STREET_NUM));
                location.setFacilityName(getValueOrEmpty(row, COL_FACILITY_NAME));
                location.setStreet(street.isEmpty() ? null : street);
                location.setZipCode(getValueOrEmpty(row, COL_ZIP_CODE));
                location.setCity(getValueOrEmpty(row, COL_CITY));

                // 3. Ajout au batch
                batch.add(mapToEntity(row, location));
                processedIds.add(row[COL_ID_PP]);

                // 4. Sauvegarde si le batch est plein
                if (batch.size() >= BATCH_SIZE) {
                    repository.saveAll(batch);
                    totalSaved += batch.size();
                    log.info("Batch sauvegardé. Total en cours : {}", totalSaved);
                    batch.clear();
                }
            }

            // 5. Enregistrer le reliquat (les derniers éléments qui n'ont pas atteint 1000)
            if (!batch.isEmpty()) {
                repository.saveAll(batch);
                totalSaved += batch.size();
            }

            log.info("[SUCCESS] Importation terminée. {} praticiens insérés ou mis à jour.", totalSaved);

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
        p.addLocation(location); // Assure-toi que cette méthode existe bien dans ton entité
        return p;
    }

    private boolean isValid(String[] row, Set<String> processedIds) {
        if (row == null || row.length < 40) {
            return false;
        }

        String rppsId = row[COL_ID_PP];
        if (processedIds.contains(rppsId)) {
            return false;
        }

        String professionCode = row[COL_PROFESSION_CODE];
        if (!ALLOWED_ROLES.contains(professionCode)) {
            return false;
        }

        if ("93".equals(professionCode)) {
            return true;
        }

        if ("10".equals(professionCode)) {
            String specialtyCode = row[COL_SPECIALTY_CODE];
            return ALLOWED_SPECIALTIES.contains(specialtyCode);
        }

        return false;
    }

    private String getValueOrEmpty(String[] row, int index) {
        return (row.length > index && row[index] != null) ? row[index].trim() : "";
    }
}
