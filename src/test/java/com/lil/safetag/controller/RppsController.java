package com.lil.safetag.controller;

import com.lil.safetag.service.RppsIngestionService;
import com.lil.safetag.entity.RppsPractitioner;
import com.lil.safetag.repository.RppsPractitionerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RppsController.class)
class RppsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RppsIngestionService ingestionService;

    @MockBean
    private RppsPractitionerRepository repository;

    @Test
    void testTriggerImport_Success() throws Exception {
        doNothing().when(ingestionService).importRppsData();

        mockMvc.perform(post("/api/v1/rpps/import"))
                .andExpect(status().isOk());
    }

    @Test
    void testTriggerImport_Error() throws Exception {
        doThrow(new RuntimeException("Erreur simulée")).when(ingestionService).importRppsData();

        mockMvc.perform(post("/api/v1/rpps/import"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetPractitionerById_Found() throws Exception {
        RppsPractitioner mockPractitioner = new RppsPractitioner();
        mockPractitioner.setRppsId("123456789");
        mockPractitioner.setName("Jean Dupont");

        when(repository.findByRppsId("123456789")).thenReturn(Optional.of(mockPractitioner));

        mockMvc.perform(get("/api/v1/rpps/123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rppsId").value("123456789"))
                .andExpect(jsonPath("$.name").value("Jean Dupont"));
    }

    @Test
    void testGetPractitionerById_NotFound() throws Exception {
        when(repository.findByRppsId(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/rpps/999999999"))
                .andExpect(status().isNotFound());
    }
}
