package com.lil.safetag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper; // Ajout de l'import
import com.lil.safetag.entity.PracticeLocation;
import com.lil.safetag.repository.PracticeLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final String BAN_API_URL = "https://api-adresse.data.gouv.fr/search/?q={query}&limit=1";

    public int processGeocodingBatch() {
        List<PracticeLocation> locations = repository.findTop50ByGeocodingAttemptedFalse();

        if (locations.isEmpty()) {
            log.info("Aucune adresse à géocoder.");
            return 0;
        }

        int processed = 0;

        for (PracticeLocation loc : locations) {
            loc.setGeocodingAttempted(true);

            String query = buildAddressQuery(loc);
            if (query.isBlank()) {
                continue;
            }

            try {
                // MODIFICATION ICI : On récupère d'abord une String
                String responseBody = restTemplate.getForObject(BAN_API_URL, String.class, query);

                if (responseBody != null) {
                    // On parse la String en JsonNode
                    JsonNode response = objectMapper.readTree(responseBody);

                    if (response.has("features") && response.get("features").size() > 0) {
                        JsonNode coordinates = response.get("features").get(0).get("geometry").get("coordinates");
                        loc.setLongitude(coordinates.get(0).asDouble());
                        loc.setLatitude(coordinates.get(1).asDouble());
                        processed++;
                    } else {
                        log.warn("Aucun résultat trouvé pour l'adresse ID {}: {}", loc.getId(), query);
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors du géocodage de l'adresse ID {}: {}", loc.getId(), e.getMessage());
            }
        }

        repository.saveAll(locations);
        return processed;
    }

    private String buildAddressQuery(PracticeLocation loc) {
        StringBuilder query = new StringBuilder();
        if (loc.getStreetNumber() != null) query.append(loc.getStreetNumber()).append(" ");
        if (loc.getStreet() != null) query.append(loc.getStreet()).append(" ");
        if (loc.getZipCode() != null) query.append(loc.getZipCode()).append(" ");
        if (loc.getCity() != null) query.append(loc.getCity());

        return query.toString().trim();
    }
}
