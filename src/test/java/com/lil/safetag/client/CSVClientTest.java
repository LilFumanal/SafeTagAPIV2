package com.lil.safetag.client;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

// Démarre un faux serveur sur le port 8089 pendant les tests
@WireMockTest(httpPort = 8089)
class CSVClientTest {

    private CSVClient csvClient;

    @BeforeEach
    void setUp() {
        // On donne l'URL de notre faux serveur local au client
        String fakeUrl = "http://localhost:8089/rpps.csv";
        csvClient = new CSVClient(fakeUrl, 1); // 1 minute de timeout
    }

    @Test
    void shouldReturnInputStream_WhenServerReturns200() throws Exception {
        // GIVEN: Le serveur retourne un faux fichier CSV
        String fakeCsvContent = "TypeIdentifiant;Identifiant;Nom;Prenom\n8;1000000000;DUPONT;Jean";
        stubFor(get(urlEqualTo("/rpps.csv"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(fakeCsvContent)));

        // WHEN: On lance le téléchargement
        InputStream resultStream = csvClient.downloadRppsFile();

        // THEN: On vérifie qu'on a bien récupéré le contenu
        assertNotNull(resultStream);
        String resultString = new String(resultStream.readAllBytes());
        assertTrue(resultString.contains("DUPONT;Jean"));
    }

    @Test
    void shouldThrowIOException_WhenServerReturns500() {
        // GIVEN: Le serveur plante (Erreur 500)
        stubFor(get(urlEqualTo("/rpps.csv"))
                .willReturn(aResponse().withStatus(500)));

        // WHEN & THEN: Une IOException doit être levée avec le bon code
        IOException exception = assertThrows(
                IOException.class,
                () -> csvClient.downloadRppsFile()
        );

        assertTrue(exception.getMessage().contains("Code HTTP : 500"));
    }
}
