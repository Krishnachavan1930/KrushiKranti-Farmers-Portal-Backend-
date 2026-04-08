package com.krushikranti.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.Arrays;

@Configuration
@Slf4j
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOriginsRaw;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] origins = Arrays.stream(allowedOriginsRaw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toArray(String[]::new);

                log.info("CORS allowed origins: {}", Arrays.toString(origins));

                registry.addMapping("/**")
                        .allowedOrigins(origins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                        .allowedHeaders("*")
                        .exposedHeaders("Authorization")
                        .allowCredentials(true)
                        .maxAge(3600);
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                String uploadDir = Path.of("uploads").toAbsolutePath().toUri().toString();
                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations(uploadDir);
            }
        };
    }
}
