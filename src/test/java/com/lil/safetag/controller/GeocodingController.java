package com.lil.safetag.controller;

import com.lil.safetag.service.GeocodingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GeocodingController.class)
class GeocodingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GeocodingService geocodingService;

    @Test
    void testTriggerGeocoding() throws Exception {
        // Préparation du comportement attendu
        when(geocodingService.processGeocodingBatch()).thenReturn(10);

        // Appel HTTP simulé et vérifications
        mockMvc.perform(post("/api/geocoding/trigger"))
                .andExpect(status().isOk())
                .andExpect(content().string("Batch de géocodage terminé. 10 adresses traitées."));
    }
}
