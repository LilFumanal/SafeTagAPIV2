package com.lil.safetagv2rppsservice.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lil.safetagv2rppsservice.config.RppsProperties;
import com.lil.safetagv2rppsservice.exception.RppsExceptions;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Slf4j
@Service
public class RppsAPIClient {

    private final RestTemplate restTemplate;
    private final RppsProperties properties;
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String RESILIENCE_INSTANCE = "rppsApi";

    public RppsAPIClient(RppsProperties properties) {
        this.restTemplate = new RestTemplate();
        this.properties = properties;
    }

    private String callUrl(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("ESANTE-API-KEY", properties.getApiKey());

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            return response.getBody();

        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().is4xxClientError()) {
                if (e.getStatusCode().value() == 404) {
                    throw new RppsExceptions.NotFoundException("Ressource RPPS non trouvée à l'URL : " + url);
                }
                throw new RppsExceptions.BaseException("Erreur client RPPS : " + e.getResponseBodyAsString());
            }
            throw new RppsExceptions.CommunicationException("Erreur serveur RPPS lors de l'appel", e);
        } catch (RestClientException e) {
            throw new RppsExceptions.CommunicationException("Impossible de contacter l'API RPPS (Timeout ou Réseau)", e);
        }
    }

    // --- Search Methods (avec Fallback & Retry) ---
    @Cacheable(value = "practitioners", key = "#rppsId")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "fallbackGetPractitionerById")
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    public Map<String, Object> getPractitionerById(String rppsId) {
        String url = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/Practitioner")
                .queryParam("identifier", rppsId)
                .build().toUriString();

        String jsonResponse = callUrl(url);
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            throw new RppsExceptions.NotFoundException("Aucune réponse de l'API pour l'ID : " + rppsId);
        }

        try {
            List<Map<String, Object>> results = parsePractitioner(mapper.readTree(jsonResponse));
            if (results.isEmpty()) {
                throw new RppsExceptions.NotFoundException("Praticien introuvable avec l'ID : " + rppsId);
            }
            return results.get(0);
        } catch (JsonProcessingException e) {
            throw new RppsExceptions.BaseException("Erreur de parsing pour le praticien ID : " + rppsId, e);
        }
    }

    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "fallbackSearchList")
    public List<Map<String, Object>> searchByName(String name) {
        String url = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/Practitioner")
                .queryParam("name", name)
                .build().toUriString();

        String jsonResponse = callUrl(url);
        if (jsonResponse == null || jsonResponse.isEmpty()) return Collections.emptyList();

        try {
            return parsePractitioner(mapper.readTree(jsonResponse));
        } catch (JsonProcessingException e) {
            throw new RppsExceptions.BaseException("Erreur de formatage JSON lors de la recherche par nom", e);
        }
    }

    // --- Fallbacks Methods ---

    public Map<String, Object> fallbackGetPractitionerById(String rppsId, Throwable t) {
        if (t instanceof RppsExceptions.NotFoundException) {
            throw (RppsExceptions.NotFoundException) t;
        }
        log.warn("Fallback activé pour getPractitionerById (API indisponible) - RPPS: {}. Erreur: {}", rppsId, t.getMessage());
        return Collections.emptyMap();
    }

    public List<Map<String, Object>> fallbackSearchList(String param, Throwable t) {
        log.warn("Fallback activé pour recherche liste (param: {}) - Erreur: {}", param, t.getMessage());
        return Collections.emptyList();
    }

    public Map<String, String> fallbackSearchMap(String param, Throwable t) {
        if (t instanceof RppsExceptions.NotFoundException) {
            throw (RppsExceptions.NotFoundException) t;
        }
        log.warn("Fallback activé pour recherche map (param: {}) - Erreur: {}", param, t.getMessage());
        return Collections.emptyMap();
    }

    // --- Parsing Methods ---

    public List<Map<String, Object>> parsePractitioner(JsonNode root) {
        List<Map<String, Object>> practitioners = new ArrayList<>();
        JsonNode entries = root.path("entry");

        if (!entries.isArray()) return practitioners;

        for (JsonNode entry : entries) {
            JsonNode resource = entry.path("resource");
            if (!"Practitioner".equals(resource.path("resourceType").asText())) continue;
            String id = resource.path("id").asText(null);

            if (id == null) continue;

            JsonNode nameNode = resource.path("name").get(0);
            String family = nameNode.path("family").asText("");
            String given = nameNode.path("given").path(0).asText("");
            String fullName = (family + " " + given).trim();

            List<String> professionCodes = new ArrayList<>();
            List<String> specialtyCodes = new ArrayList<>();

            for (JsonNode q : resource.path("qualification")) {
                for (JsonNode coding : q.path("code").path("coding")) {
                    String system = coding.path("system").asText("");
                    String code = coding.path("code").asText(null);

                    if (code != null) {
                        if (system.contains("TRE_G15")) professionCodes.add(code);
                        else if (system.contains("TRE_R38")) specialtyCodes.add(code);
                    }
                }
            }

            Map<String, Object> practitioner = new HashMap<>();
            practitioner.put("id", id);
            practitioner.put("name", fullName);
            practitioner.put("professionCodes", professionCodes);
            practitioner.put("specialtyCodes", specialtyCodes);

            practitioners.add(practitioner);
        }
        return practitioners;
    }

    public Map<String, String> parsePractitionerRole(JsonNode root) {
        int total = root.path("total").asInt();
        if (total == 0) return null;

        for (JsonNode entry : root.path("entry")) {
            JsonNode resource = entry.path("resource");

            if (!resource.path("active").asBoolean(false)) continue;

            Map<String, String> roleData = new HashMap<>();
            String organizationId = resource.path("organization").path("identifier").path("value").asText(null);
            roleData.put("organizationId", organizationId);

            for (JsonNode code : resource.path("code")) {
                for (JsonNode coding : code.path("coding")) {
                    String system = coding.path("system").asText();
                    String display = coding.path("display").asText();

                    if (system.contains("TRE_R22")) roleData.put("genreActivite", display);
                    else if (system.contains("TRE_R23")) roleData.put("modeExercice", display);
                    else if (system.contains("TRE_R21")) roleData.put("fonction", display);
                }
            }
            return roleData;
        }
        return null;
    }

    public Map<String, String> parseOrganization(JsonNode root) {
        JsonNode entries = root.path("entry");
        if (!entries.isArray() || entries.isEmpty()) return null;

        JsonNode addressArray = entries.get(0).path("resource").path("address");
        if (!addressArray.isArray() || addressArray.isEmpty()) return null;

        JsonNode address = addressArray.get(0);
        Map<String, String> result = new HashMap<>();
        result.put("city", address.path("city").asText(null));
        result.put("postalCode", address.path("postalCode").asText(null));

        return result;
    }
    RestTemplate getRestTemplate() {
        return this.restTemplate;
    }
}
