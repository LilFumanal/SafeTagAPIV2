package com.lil.safetag.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RppsClient {

    private final RestTemplate restTemplate;
    private final RppsProperties properties;

    public RppsClient(RppsProperties properties) {
        this.restTemplate = new RestTemplate();
        this.properties = properties;
    }

    private @Nullable String callUrl(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("ESANTE-API-KEY", properties.getApiKey());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );

        return response.getBody();
    }

    public List<Map<String, String>> searchByName(String name) {
        String url = properties.getBaseUrl() + "/Practitioner?name=" + name;
        String data = callUrl(url);
        return parsePractitionerBody(data);
    }

    public Map<String, String> searchPractitionerRole(String practitionnerId) {
        String url = properties.getBaseUrl() + "/PractitionerRole?practitioner=" + practitionnerId;
        String data = callUrl(url);
        return parsePractitionerRole(data);
    }

    public List<Map<String, String>> parsePractitionerBody(String response) {
        List<Map<String, String>> practitioners= new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode entries = root.path("entry");

            for (JsonNode entry : entries) {
                JsonNode resource = entry.path("resource");

                String id = resource.path("id").asText(null);

                JsonNode nameNode = resource.path("name").isArray()
                        ? resource.path("name").get(0)
                        : null;

                String family = nameNode != null ? nameNode.path("family").asText("") : "";
                String given = (nameNode != null && nameNode.path("given").isArray())
                        ? nameNode.path("given").get(0).asText("")
                        : "";

                String fullName = (family + " " + given).trim();
                String profession = null;
                String specialite = null;

                JsonNode qualifications = resource.path("qualification");
                for (JsonNode q : qualifications) {
                    JsonNode codings = q.path("code").path("coding");

                    for (JsonNode coding : codings) {
                        String system = coding.path("system").asText();

                        if (system.contains("TRE_G15")) {
                            profession = coding.path("display").asText(null);
                        } else if (system.contains("TRE_R38")) {
                            specialite = coding.path("display").asText(null);
                        }
                    }
                }

                if (id != null) {
                    Map<String, String> practitioner = new HashMap<>();
                    practitioner.put("id", id);
                    practitioner.put("name", fullName);
                    practitioner.put("profession", profession);
                    practitioner.put("specialite", specialite);

                    practitioners.add(practitioner);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur parsing RPPS", e);
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

        } catch (Exception e) {
            throw new RuntimeException("Erreur parsing RPPS", e);
        }
    }
    public Map<String, String> searchOrganization(String organizationId) {
        System.out.println(organizationId);
        String url = properties.getBaseUrl() + "/Organization?identifier=" + organizationId;
        String data = callUrl(url);

        System.out.println(callUrl(url));

        return parseOrganization(data);
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

        } catch (Exception e) {
            throw new RuntimeException("Erreur parsing Organization RPPS", e);
        }
    }
}
