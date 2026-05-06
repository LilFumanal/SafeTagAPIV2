package com.lil.safetagv2rppsservice.service;

import com.lil.safetagv2rppsservice.client.RppsAPIClient;
import com.lil.safetagv2rppsservice.dto.PractitionerDTO;
import com.lil.safetagv2rppsservice.entity.PracticeLocation;
import com.lil.safetagv2rppsservice.entity.RppsPractitioner;
import com.lil.safetagv2rppsservice.repository.RppsPractitionerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PractitionerServiceTest {

    @Mock
    private RppsAPIClient rppsAPIClient;

    @Mock
    private RppsPractitionerRepository repository;

    @InjectMocks
    private PractitionerService practitionerService;

    private RppsPractitioner mockPractitioner;

    @BeforeEach
    void setUp() {
        mockPractitioner = new RppsPractitioner();
        mockPractitioner.setRppsId("123456789");
        mockPractitioner.setName("DUPONT Jean");
        mockPractitioner.setProfessionCode("10");
        mockPractitioner.setExerciceMode("Libéral");

        PracticeLocation location = new PracticeLocation();
        location.setCity("Paris");
        mockPractitioner.setLocations(List.of(location));
    }

    @Test
    void searchPractitioners_ShouldReturnMappedPage() {
        // GIVEN
        PageRequest pageable = PageRequest.of(0, 10);
        Page<RppsPractitioner> pagedResponse = new PageImpl<>(List.of(mockPractitioner));

        when(repository.searchPractitioners(
                eq("%dupont%"), eq("10"), isNull(), eq("%paris%"),
                eq(false),  eq(0.0), eq(0.0), eq(0.0), eq(pageable)
        )).thenReturn(pagedResponse);

        // WHEN
        Page<PractitionerDTO> result = practitionerService.searchPractitioners(
                "Dupont", "10", null, "Paris", null, null, null, pageable
        );

        // THEN
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("DUPONT Jean", result.getContent().get(0).name());
        assertEquals("Paris", result.getContent().get(0).practiceAddresses().get(0).city());
    }

    @Test
    void getAndUpdatePractitioner_ApiSuccess_ShouldUpdateAndReturn() throws Exception {
        // GIVEN
        String rppsId = "12345678910";
        Map<String, Object> apiData = new HashMap<>();
        apiData.put("name", "DUPONT Jean API");
        apiData.put("professionCodes", List.of("10"));

        when(rppsAPIClient.getPractitionerById(rppsId)).thenReturn(apiData);
        when(repository.findByRppsId(rppsId)).thenReturn(Optional.of(mockPractitioner));
        when(repository.save(any(RppsPractitioner.class))).thenAnswer(i -> i.getArguments()[0]);

        // WHEN
        PractitionerDTO result = practitionerService.getAndUpdatePractitioner(rppsId);

        // THEN
        assertNotNull(result);
        assertEquals("DUPONT Jean API", result.name()); // Le nom a été mis à jour par l'API
        verify(repository, times(1)).save(any(RppsPractitioner.class)); // Vérifie la sauvegarde
    }

    @Test
    void getAndUpdatePractitioner_ApiFails_ShouldFallbackToLocal() throws Exception {
        // GIVEN
        String rppsId = "12345678910";
        when(rppsAPIClient.getPractitionerById(rppsId)).thenThrow(new RuntimeException("API Down"));
        when(repository.findByRppsId(rppsId)).thenReturn(Optional.of(mockPractitioner));

        // WHEN
        PractitionerDTO result = practitionerService.getAndUpdatePractitioner(rppsId);

        // THEN
        assertNotNull(result);
        assertEquals("DUPONT Jean", result.name()); // On a gardé le nom local
        verify(repository, never()).save(any(RppsPractitioner.class)); // Pas de sauvegarde si fallback
    }

    @Test
    void getAndUpdatePractitioner_ApiFailsAndNoLocal_ShouldThrowException() throws Exception {
        // GIVEN
        String rppsId = "99999999999";
        when(rppsAPIClient.getPractitionerById(rppsId)).thenThrow(new RuntimeException("API Down"));
        when(repository.findByRppsId(rppsId)).thenReturn(Optional.empty());

        // WHEN / THEN
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            practitionerService.getAndUpdatePractitioner(rppsId);
        });
        assertTrue(exception.getMessage().contains("introuvable localement et API e-santé indisponible"));
    }
}
