package com.lil.safetag.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;


@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
    @Bean
    public WebClient rppsWebClient() {
        // Ici, on configure la limite de mémoire pour ce WebClient spécifique
        HttpClient httpClient = HttpClient.create().followRedirect(true)
                .responseTimeout(Duration.ofSeconds(60)); // Exemple de timeout

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // --- C'est ici qu'il faut ajouter la configuration ---
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024 * 100)) // 100MB par exemple
                        .build())
                .build();
    }
}
