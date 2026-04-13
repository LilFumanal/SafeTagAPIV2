package com.lil.safetag.controller;

import com.lil.safetag.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/geocoding")
@RequiredArgsConstructor
public class GeocodingController {

    private final GeocodingService geocodingService;

    @PostMapping("/trigger")
    public ResponseEntity<String> triggerGeocoding() {
        int processedCount = geocodingService.processGeocodingBatch();
        return ResponseEntity.ok("Batch de géocodage terminé. " + processedCount + " adresses traitées.");
    }
}
