package com.lil.safetag.client;

import com.lil.safetag.exception.RppsExceptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RppsAPIClientTest {

    private RppsAPIClient apiClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        // 1. On simule les propriétés (sans charger tout le contexte Spring)
        RppsProperties properties = new RppsProperties();
        properties.setBaseUrl("https://api.esante.gouv.fr");
        properties.setApiKey("fake-api-key");

        // 2. On instancie le client
        apiClient = new RppsAPIClient(properties);

        // 3. On branche le faux serveur sur le RestTemplate du client
        mockServer = MockRestServiceServer.createServer(apiClient.getRestTemplate());
    }

    @Test
    void shouldReturnPractitioner_WhenApiReturnsValidJson() {
        // GIVEN: Le faux JSON que l'API e-santé est censée retourner
        String mockJsonResponse = """
        {
          "resourceType": "Bundle",
          "entry": [
            {
              "resource": {
                "resourceType": "Practitioner",
                "id": "810000000000",
                "name": [
                  { "family": "DUPONT", "given": ["Jean"] }
                ],
                "qualification": []
              }
            }
          ]
        }
        """;

        // GIVEN: On indique au faux serveur quoi répondre quand on l'appelle
        mockServer.expect(requestTo("https://api.esante.gouv.fr/Practitioner?identifier=810000000000"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("ESANTE-API-KEY", "fake-api-key"))
                .andRespond(withSuccess(mockJsonResponse, MediaType.APPLICATION_JSON));

        // WHEN: On appelle notre méthode métier
        Map<String, Object> result = apiClient.getPractitionerById("810000000000");

        // THEN: On vérifie que le JSON a été correctement parsé dans la Map
        assertNotNull(result);
        assertEquals("810000000000", result.get("id"));
        assertEquals("DUPONT Jean", result.get("name"));

        // Vérifie que le serveur a bien été appelé comme prévu
        mockServer.verify();
    }

    @Test
    void shouldThrowNotFoundException_WhenApiReturns404() {
        // GIVEN: Le serveur retourne une erreur 404
        mockServer.expect(requestTo("https://api.esante.gouv.fr/Practitioner?identifier=999"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("Not Found"));

        // WHEN & THEN: On s'attend à ce que NOTRE exception personnalisée soit levée
        RppsExceptions.NotFoundException exception = assertThrows(
                RppsExceptions.NotFoundException.class,
                () -> apiClient.getPractitionerById("999")
        );

        assertTrue(exception.getMessage().contains("Ressource RPPS non trouvée"));
        mockServer.verify();
    }
}
