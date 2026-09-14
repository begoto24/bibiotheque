package com.ibizabroker.bibliotheque.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfiguration {

    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String PATCH = "PATCH";
    private static final String DELETE = "DELETE";

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        // PATCH manquait : le module Réservation (annulation, PATCH
                        // /api/reservations/{id}/annuler) l'utilise, mais cette config
                        // datait d'avant son ajout. Le préflight CORS refusait la
                        // méthode -> le navigateur bloquait la requête avant même de
                        // l'envoyer (0 octet transféré, "impossible de contacter le
                        // serveur" côté frontend, quel que soit le rôle connecté).
                        .allowedMethods(GET, POST, PUT, PATCH, DELETE)
                        .allowedHeaders("*")
                        .allowedOriginPatterns("*")
                        .allowCredentials(true);
            }
        };
    }
}