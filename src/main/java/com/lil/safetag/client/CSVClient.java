package com.lil.safetag.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@Component
public class CSVClient {

    private final WebClient webClient;
    private final String rppsUrl;
    private final int timeoutMinutes;

    public CSVClient(@Qualifier("rppsWebClient") WebClient rppsWebClient,
                     @Value("${rpps.source.csvUrl}") String rppsUrl,
                     @Value("${rpps.source.timeout-minutes}") int timeoutMinutes) {
        this.webClient = rppsWebClient;
        this.rppsUrl = rppsUrl;
        this.timeoutMinutes = timeoutMinutes;

        System.out.println("URL CHARGÉE PAR SPRING : [" + this.rppsUrl + "]"); //
    }

    /**
     * Récupère le flux d'entrée du fichier distant (GZ ou CSV).
     * On utilise Mono<Resource> pour streamer le contenu sans le stocker en RAM.
     */
    public InputStream downloadRppsFile() throws IOException {
        return webClient.get()
                .uri(rppsUrl)
                .retrieve()
                .bodyToMono(Resource.class)
                .map(resource -> {
                    try {
                        return resource.getInputStream();
                    } catch (IOException e) {
                        throw new RuntimeException("Erreur lors de l'ouverture du flux RPPS", e);
                    }
                })
                .block(Duration.ofMinutes(timeoutMinutes));
    }
}