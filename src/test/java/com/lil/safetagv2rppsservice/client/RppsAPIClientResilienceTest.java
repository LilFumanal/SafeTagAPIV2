package com.lil.safetagv2rppsservice.client;

import com.lil.safetagv2rppsservice.exception.RppsExceptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@SpringBootTest
class RppsAPIClientResilienceTest {

    @Autowired
    private RppsAPIClient rppsAPIClient;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        // On récupère le RestTemplate interne pour le mocker
        RestTemplate restTemplate = rppsAPIClient.getRestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void testGetPractitionerById_RetryAndFallback_OnServerError() {
        String rppsId = "81000000000";
        String expectedUrl = "https://gateway.api.esante.gouv.fr/fhir/v2/Practitioner?identifier=81000000000";

        // On configure le mock pour qu'il réponde en erreur 500 EXACTEMENT 3 fois (maxAttempts)
        mockServer.expect(ExpectedCount.times(3), requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        // Exécution de la méthode
        Map<String, Object> result = rppsAPIClient.getPractitionerById(rppsId);

        // Vérifications
        mockServer.verify(); // Vérifie que l'API a bien été appelée 3 fois
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Le fallback doit retourner une map vide");
    }

    @Test
    void testGetPractitionerById_NoRetry_OnNotFoundError() {
        String rppsId = "99999999999";
        String expectedUrl = "https://gateway.api.esante.gouv.fr/fhir/v2/Practitioner?identifier=99999999999";

        // On configure le mock pour répondre en 404 UNE SEULE FOIS (pas de retry)
        mockServer.expect(ExpectedCount.once(), requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // Exécution et vérification que l'exception remonte (pas de fallback)
        assertThrows(RppsExceptions.NotFoundException.class, () -> {
            rppsAPIClient.getPractitionerById(rppsId);
        });

        mockServer.verify(); // Vérifie que l'API n'a été appelée qu'une seule fois
    }
}
