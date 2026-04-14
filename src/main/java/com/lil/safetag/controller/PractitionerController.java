package com.lil.safetag.controller;

import com.lil.safetag.dto.PractitionerDTO;
import com.lil.safetag.service.PractitionerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/practitioners") // On garde ton chemin initial pour ne pas casser tes tests
public class PractitionerController {

    private final PractitionerService practitionerService;

    public PractitionerController(PractitionerService practitionerService) {
        this.practitionerService = practitionerService;
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PractitionerDTO>> searchPractitioners(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String professionCode,
            @RequestParam(required = false) String specialtyCode,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Double radiusKm,
            Pageable pageable) {

        return ResponseEntity.ok(practitionerService.searchPractitioners(
                name, professionCode, specialtyCode, city, lat, lon, radiusKm, pageable));
    }

    // 2. Consultation unitaire : Base locale + Mise à jour API e-santé (Opportuniste)
    @GetMapping("/{rppsId}")
    public PractitionerDTO getPractitioner(@PathVariable String rppsId) {
        return practitionerService.getAndUpdatePractitioner(rppsId);
    }
}