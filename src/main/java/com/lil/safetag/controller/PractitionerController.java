package com.lil.safetag.controller;

import com.lil.safetag.client.RppsClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/practitioners")
public class PractitionerController {

    private final RppsClient rppsClient;

    public PractitionerController(RppsClient rppsClient) {
        this.rppsClient = rppsClient;
    }

    @GetMapping
    public List<Map<String, String>> search(@RequestParam String name) {
        List<Map<String, String>> practitioners = rppsClient.searchByName(name);

        for (Map<String, String> practitioner : practitioners) {
            String id = practitioner.get("id");

            Map<String, String> role = rppsClient.searchPractitionerRole(id);
            System.out.println(role);
            System.out.println(role.size());

            if (role != null) {
                practitioner.putAll(role);

                String orgId = role.get("organizationId");
                System.out.println("ORG ID = " + orgId);

                if (orgId != null) {
                    Map<String, String> org = rppsClient.searchOrganization(orgId);
                    System.out.println(org);

                    if (org != null) {
                        practitioner.putAll(org);
                    }
                }
            }
        }

        return practitioners;
    }
}

