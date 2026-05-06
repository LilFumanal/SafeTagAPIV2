package com.lil.safetagv2rppsservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper; // Ajout de l'import
import com.lil.safetagv2rppsservice.entity.PracticeLocation;
import com.lil.safetagv2rppsservice.repository.PracticeLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final PracticeLocationRepository repository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rpps.source.geocodingAPIUrl}")
    private String banApiUrl;

    public int processGeocodingBatch() {
        List<PracticeLocation> locations = repository.findTop50ByGeocodingAttemptedFalse();

        if (locations.isEmpty()) {
            log.info("Aucune adresse à géocoder.");
            return 0;
        }
        log.info("Géocodage d'un lot de " + locations.size() + " adresses...");
        int successCount = 0;

        for (PracticeLocation loc : locations) {
            loc.setGeocodingAttempted(true);

            String query = buildAddressQuery(loc);
            if (query.isBlank()) {
                continue;
            }

            try {
                String responseBody = restTemplate.getForObject(banApiUrl , String.class, query);

                if (responseBody != null) {
                    JsonNode response = objectMapper.readTree(responseBody);

                    if (response.has("features") && response.get("features").size() > 0) {
                        JsonNode coordinates = response.get("features").get(0).get("geometry").get("coordinates");
                        loc.setLongitude(coordinates.get(0).asDouble());
                        loc.setLatitude(coordinates.get(1).asDouble());
                        successCount++; // Incrémente les succès
                    } else {
                        log.warn("Aucun résultat trouvé pour l'adresse ID {}: {}", loc.getId(), query);
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors du géocodage de l'adresse ID {}: {}", loc.getId(), e.getMessage());
            }
        }

        repository.saveAll(locations);
        log.info("Batch terminé : {} adresses trouvées sur {} tentées.", successCount, locations.size());

        return locations.size(); // On retourne le nombre d'adresses traitées (50)
    }

    private String buildAddressQuery(PracticeLocation loc) {
        StringBuilder query = new StringBuilder();
        if (loc.getStreetNumber() != null) query.append(loc.getStreetNumber()).append(" ");
        if (loc.getStreet() != null) query.append(loc.getStreet()).append(" ");
        if (loc.getZipCode() != null) query.append(loc.getZipCode()).append(" ");
        if (loc.getCity() != null) query.append(loc.getCity());

        return query.toString().trim();
    }

    @Scheduled(fixedDelay = 5000)
    public void scheduledGeocodingTask() {
        int processed = processGeocodingBatch();

        if (processed == 0) {
            // Optionnel : on pourrait désactiver la tâche ici si tout est fini,
            // mais un appel à la BDD toutes les 5s qui retourne vide est négligeable.
            log.debug("Tâche de géocodage en attente : aucune nouvelle adresse.");
        }
    }
}
