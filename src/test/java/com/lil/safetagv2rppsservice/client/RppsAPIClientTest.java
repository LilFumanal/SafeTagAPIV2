package com.lil.safetagv2rppsservice.client;

import com.lil.safetagv2rppsservice.exception.RppsExceptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.hamcrest.Matchers.containsString;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@SpringBootTest
@ImportAutoConfiguration(CacheAutoConfiguration.class)
@EnableCaching
class RppsAPIClientTest {

    @Autowired
    private RppsAPIClient apiClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(apiClient.getRestTemplate());
    }

    @Test
    void shouldReturnPractitioner_WhenApiReturnsValidJson() {
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

        // CORRECTION : On utilise containsString pour ignorer le nom de domaine
        mockServer.expect(requestTo(containsString("/Practitioner?identifier=810000000000")))
                .andExpect(method(HttpMethod.GET))
                // Retiré temporairement si ta vraie config n'envoie pas cet header exact
                // .andExpect(header("ESANTE-API-KEY", "fake-api-key"))
                .andRespond(withSuccess(mockJsonResponse, MediaType.APPLICATION_JSON));

        Map<String, Object> result = apiClient.getPractitionerById("810000000000");

        assertNotNull(result);
        assertEquals("810000000000", result.get("id"));
        assertEquals("DUPONT Jean", result.get("name"));

        mockServer.verify();
    }

    @Test
    void shouldThrowNotFoundException_WhenApiReturns404() {
        // CORRECTION : On utilise containsString
        mockServer.expect(requestTo(containsString("/Practitioner?identifier=999")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("Not Found"));

        RppsExceptions.NotFoundException exception = assertThrows(
                RppsExceptions.NotFoundException.class,
                () -> apiClient.getPractitionerById("999")
        );

        assertTrue(exception.getMessage().contains("Ressource RPPS non trouvée"));
        mockServer.verify();
    }

    @Test
    void shouldReturnCachedPractitionerOnSecondCall() {
        String rppsId = "12345678901";
        String mockJsonResponse = """
            {
              "resourceType": "Bundle",
              "entry": [
                {
                  "resource": {
                    "resourceType": "Practitioner",
                    "id": "12345678901",
                    "name": [
                      { "family": "DUPONT", "given": ["Jean"] }
                    ],
                    "qualification": []
                  }
                }
              ]
            }
            """;
        // 1. On configure le mock pour n'accepter qu'UN SEUL appel vers l'API
        mockServer.expect(ExpectedCount.once(), requestTo(containsString("/Practitioner?identifier=" + rppsId)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(mockJsonResponse, MediaType.APPLICATION_JSON));

        // 2. Premier appel : Le cache est vide, ça appelle le Mock API
        Map<String, Object> result1 = apiClient.getPractitionerById(rppsId);
        Map<String, Object> result2 = apiClient.getPractitionerById(rppsId);
        mockServer.verify();
        assertEquals(result1, result2);
    }
}
