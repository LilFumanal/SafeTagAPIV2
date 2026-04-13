package com.lil.safetag.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class CSVClient {

    private final String rppsUrl;
    private final int timeoutMinutes;

    public CSVClient(@Value("${rpps.source.csvUrl}") String rppsUrl,
                     @Value("${rpps.source.timeout-minutes}") int timeoutMinutes) {
        this.rppsUrl = rppsUrl;
        this.timeoutMinutes = timeoutMinutes;
    }

    /**
     * Récupère le flux d'entrée du fichier distant (GZ ou CSV).
     * On utilise Mono<Resource> pour streamer le contenu sans le stocker en RAM.
     */
    public InputStream downloadRppsFile() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30)) //Timeout connexion
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(rppsUrl))
                .timeout(Duration.ofMinutes(timeoutMinutes)) // Timeout global
                .GET()
                .build();

        // ofInputStream() stream directement la réponse HTTP
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IOException("Échec du téléchargement RPPS. Code HTTP : " + response.statusCode());
        }

        return response.body();
    }
}