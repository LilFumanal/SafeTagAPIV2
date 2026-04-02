package com.lil.safetag.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lil.safetag.exception.RppsExceptions;
import org.jspecify.annotations.Nullable;
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

@Service
public class RppsClient {

    private final RestTemplate restTemplate;
    private final RppsProperties properties;
    private static final ObjectMapper mapper = new ObjectMapper();

    public RppsClient(RppsProperties properties) {
        this.restTemplate = new RestTemplate();
        this.properties = properties;
    }

    private @Nullable String callUrl(String url) {
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


    public List<Map<String, Object>> searchByName(String name) {
        // 1. Construction de l'URL
        String url = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/Practitioner")
                .queryParam("name", name)
                .build()
                .toUriString();

        // 2. Appel via ta méthode callUrl (qui gère l'API Key)
        String jsonResponse = callUrl(url);

        // 3. Traitement de la réponse
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // On utilise ton 'mapper' statique existant
            JsonNode root = mapper.readTree(jsonResponse);
            return parsePractitioner(root);
        } catch (JsonProcessingException e) {
            throw new RppsExceptions.BaseException("Erreur de formatage JSON lors de la recherche par nom", e);
        }
    }

    public Map<String, String> searchPractitionerRole(String practitionerId) {
        String url = properties.getBaseUrl() + "/PractitionerRole?practitioner=" + practitionerId;
        String data = callUrl(url);
        Map<String, String> role = parsePractitionerRole(data);
        if (role == null) {
            throw new RppsExceptions.NotFoundException("Aucun rôle actif trouvé pour le praticien : " + practitionerId);
        }
        return role;
    }

    public Map<String, String> searchOrganization(String organizationId) {
        String url = properties.getBaseUrl() + "/Organization?identifier=" + organizationId;
        String data = callUrl(url);
        Map<String, String> org = parseOrganization(data);
        if (org == null) {
            throw new RppsExceptions.NotFoundException("Organisation non trouvée : " + organizationId);
        }
        return org;
    }
    // --- Parsing Methods ---
    public List<Map<String, Object>> parsePractitioner(JsonNode root) {
        List<Map<String, Object>> practitioners = new ArrayList<>();

        // Plus besoin de mapper.readTree() car le travail est déjà fait
        JsonNode entries = root.path("entry");

        if (!entries.isArray()) {
            return practitioners;
        }

        for (JsonNode entry : entries) {
            JsonNode resource = entry.path("resource");
            String id = resource.path("id").asText(null);

            if (id == null) continue;

            // Extraction du nom (plus court avec path)
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
                        if (system.contains("TRE_G15")) {
                            professionCodes.add(code);
                        } else if (system.contains("TRE_R38")) {
                            specialtyCodes.add(code);
                        }
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

    public Map<String, String> parsePractitionerRole(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            int total = root.path("total").asInt();
            if (total == 0) {
                return null;
            }

            JsonNode entries = root.path("entry");
            for (JsonNode entry : entries) {
                JsonNode resource = entry.path("resource");

                // ✅ filtre active
                boolean active = resource.path("active").asBoolean(false);
                if (!active) {
                    continue;
                }
                Map<String, String> roleData = new HashMap<>();
                // ✅ organisation
                String organizationId = resource
                        .path("organization")
                        .path("identifier")
                        .path("value")
                        .asText(null);

                roleData.put("organizationId", organizationId);

                JsonNode codes = resource.path("code");

                for (JsonNode code : codes) {
                    JsonNode codings = code.path("coding");

                    for (JsonNode coding : codings) {
                        String system = coding.path("system").asText();
                        String display = coding.path("display").asText();

                        if (system.contains("TRE_R22")) {
                            roleData.put("genreActivite", display);
                        } else if (system.contains("TRE_R23")) {
                            roleData.put("modeExercice", display);
                        } else if (system.contains("TRE_R21")) {
                            roleData.put("fonction", display);
                        }
                    }
                }
                return roleData; // ✅ premier actif uniquement
            }

            return null;

        } catch (JsonProcessingException e) {
            throw new RppsExceptions.BaseException("Erreur parsing PractitionerRole", e);
        }
    }

    public Map<String, String> parseOrganization(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            JsonNode entries = root.path("entry");
            if (!entries.isArray() || entries.isEmpty()) return null;

            JsonNode resource = entries.get(0).path("resource");

            JsonNode addressArray = resource.path("address");
            if (!addressArray.isArray() || addressArray.isEmpty()) return null;

            JsonNode address = addressArray.get(0);

            String city = address.path("city").asText(null);
            String postalCode = address.path("postalCode").asText(null);

            Map<String, String> result = new HashMap<>();
            result.put("city", city);
            result.put("postalCode", postalCode);

            return result;

        }  catch (JsonProcessingException e) {
            throw new RppsExceptions.BaseException("Erreur parsing Organization", e);
        }
    }
}
