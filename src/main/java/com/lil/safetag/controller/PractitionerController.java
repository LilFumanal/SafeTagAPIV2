package com.lil.safetag.controller;

import com.lil.safetag.client.RppsAPIClient;
import com.lil.safetag.dto.PractitionerDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/practitioners")
public class PractitionerController {

    private final RppsAPIClient rppsAPIClient;
    private static final List<String> ALLOWED_ROLES = List.of("10", "93");
    private static final List<String> ALLOWED_SPECIALTIES =
            List.of("SM33", "SM42", "SM43", "SM92", "SM93");

    public PractitionerController(RppsAPIClient rppsAPIClient) {
        this.rppsAPIClient = rppsAPIClient;
    }

    @GetMapping("/search")
    public List<PractitionerDTO> searchByLocation(@RequestParam String location) {
        List<Map<String, Object>> rawPractitioners = rppsAPIClient.searchByLocation(location);
        return processAndFilter(rawPractitioners);
    }

    // On peut aussi modifier l'ancien search pour utiliser cette méthode commune
    @GetMapping
    public List<PractitionerDTO> searchByName(@RequestParam String name) {
        List<Map<String, Object>> rawPractitioners = rppsAPIClient.searchByName(name);
        return processAndFilter(rawPractitioners);
    }

    private List<PractitionerDTO> processAndFilter(List<Map<String, Object>> rawList) {
        List<PractitionerDTO> result = new ArrayList<>();

        for (Map<String, Object> practitionerMap : rawList) {
            if (isRelevantPractitioner(practitionerMap)) {
                String id = (String) practitionerMap.get("id");

                // Enrichissement (Appels supplémentaires)
                Map<String, String> role = rppsAPIClient.searchPractitionerRole(id);
                Map<String, String> org = null;

                if (role != null) {
                    String orgId = role.get("organizationId");
                    if (orgId != null) {
                        org = rppsAPIClient.searchOrganization(orgId);
                    }
                }

                result.add(mapToDTO(practitionerMap, role, org));
            }
        }
        return result;
    }

    private boolean isRelevantPractitioner(Map<String, Object> practitioner) {
        List<String> roleCodes = (List<String>) practitioner.get("professionCodes");
        System.out.println(roleCodes);
        List<String> specialtyCodes = (List<String>) practitioner.get("specialtyCodes");
        System.out.println(specialtyCodes);

        boolean hasValidRole = roleCodes != null &&
                roleCodes.stream().anyMatch(ALLOWED_ROLES::contains);

        boolean hasValidSpecialty = specialtyCodes != null &&
                specialtyCodes.stream().anyMatch(ALLOWED_SPECIALTIES::contains);

        return hasValidRole && hasValidSpecialty;
    }

    private PractitionerDTO mapToDTO(Map<String, Object> raw, Map<String, String> role, Map<String, String> org) {
        return new PractitionerDTO(
                (String) raw.get("id"),
                (String) raw.get("name"),
                (List<String>) raw.get("professionCodes"),
                (List<String>) raw.get("specialtyCodes"),
                (List<String>) raw.get("city"),
                (List<String>) raw.get("postalCode"),
                role != null ? role.get("modeExercice") : "Inconnu"
        );
    }
}
