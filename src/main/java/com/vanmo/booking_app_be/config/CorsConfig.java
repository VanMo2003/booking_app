package com.vanmo.booking_app_be.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.*;

/**
 * CORS for the Flutter web build, which runs on its own origin (localhost:xxxxx) and therefore sends a preflight
 * OPTIONS before every request that carries a token.
 *
 * <p>The MVC mapping alone is not enough: a preflight reaches the Spring Security filter chain first, and an
 * OPTIONS request carries no Authorization header, so without {@code .cors(...)} in {@code SecurityConfig} it is
 * rejected as unauthenticated and the browser never sends the real request. The bean below is what that
 * {@code .cors(...)} reads, so both halves have to stay together.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Any origin: this is a thesis project run from a laptop, where the web build's port changes every run.
        // Credentials stay off, so "*" is allowed and no cookie can be replayed from another site.
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
