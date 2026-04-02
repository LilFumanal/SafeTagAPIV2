package com.lil.safetag.controller;

import com.lil.safetag.client.RppsClient;
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

    private final RppsClient rppsClient;
    private static final List<String> ALLOWED_ROLES = List.of("10", "93");
    private static final List<String> ALLOWED_SPECIALTIES =
            List.of("SM33", "SM42", "SM43", "SM92", "SM93");

    public PractitionerController(RppsClient rppsClient) {
        this.rppsClient = rppsClient;
    }

    @GetMapping
    public List<PractitionerDTO> search(@RequestParam String name) {
        List<Map<String, Object>> rawPractitioners = rppsClient.searchByName(name);
        List<PractitionerDTO> result = new ArrayList<>();

        for (Map<String, Object> practitionerMap : rawPractitioners) {

            // 1. Filtrage métier (Point 7 & 8 du Decision Log)
            if (isRelevantPractitioner(practitionerMap)) {

                String id = (String) practitionerMap.get("id");

                // 2. Enrichissement (Appels supplémentaires)
                Map<String, String> role = rppsClient.searchPractitionerRole(id);
                Map<String, String> org = null;

                if (role != null) {
                    String orgId = role.get("organizationId");
                    if (orgId != null) {
                        org = rppsClient.searchOrganization(orgId);
                    }
                }

                // 3. Mapping vers le DTO (Point 5)
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
                org != null ? org.get("city") : "Non renseigné",
                org != null ? org.get("postalCode") : "N/A",
                role != null ? role.get("modeExercice") : "Inconnu"
        );
    }
}
