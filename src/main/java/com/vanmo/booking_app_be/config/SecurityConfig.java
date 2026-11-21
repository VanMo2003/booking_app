package com.vanmo.booking_app_be.config;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${jwt.signer-key}")
    private String SIGNER_KEY;

    private final String[] PUBLIC_ENDPOINT_GET = {
        "/hotels",
        "/hotels/detail/{id}",
        "/hotels/category",
        "/hotels/search",
        "/rooms",
        "/rooms/{id}",
        "/rooms/available",
        "/amenities/hotel/{hotelId}",
        "/amenities/room",
        "/services",
        "/dishes",
        "/dishes/{id}",
        // A branch's reviews are part of its page, like its rooms and its menu.
        "/feedbacks/hotel/{hotelId}",
        "/tours",
        "/tours/{id}",
        "/tours/search",
        "/tours/destinations",
        "/tours/home",
        "/vouchers/available",
        "/tours/{id}/rooms/available",
        "/app-links/{target}",
        "/app-links/tours/{id}",
        "/ai/status",
        "/ai/hotels/{hotelId}/review-digest"
    };
    private final String[] PUBLIC_ENDPOINT_POST = {
        "/accounts",
        "/owner-registrations",
        "/auth/login",
        "/auth/introspect",
        "/auth/refreshToken",
        "/storage/upload",
        "/ai/search",
        "/ai/tours/search",
        "/ai/trip-plan",
        // Asking about one branch is part of browsing it: same surface as GET /hotels/detail/{id}.
        "/ai/hotels/*/ask",
        // Comparing branches a guest is already browsing is browsing too.
        "/ai/hotels/compare",
        // Reading a branch's own page in another language is still reading its page.
        "/ai/hotels/*/translate"
    };

    private final CustomJwtDecoder customJwtDecoder;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/uploads/**");
    }

    public SecurityConfig(CustomJwtDecoder customJwtDecoder) {
        this.customJwtDecoder = customJwtDecoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(request -> request.requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINT_POST)
                .permitAll()
                // The chat socket authenticates its own upgrade from ?token= (ChatHandshakeInterceptor).
                .requestMatchers("/ws/chat")
                .permitAll()
                .requestMatchers(HttpMethod.GET, PUBLIC_ENDPOINT_GET)
                .permitAll()
                .anyRequest()
                .authenticated());

        httpSecurity.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwtConfigurer -> jwtConfigurer
                        .decoder(customJwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint()));

        // The Flutter web build is a different origin, so every authenticated call is preceded by a preflight
        // OPTIONS that carries no token. Without this the preflight is answered 401 and the browser gives up.
        httpSecurity.cors(Customizer.withDefaults());

        httpSecurity.csrf(AbstractHttpConfigurer::disable);

        return httpSecurity.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return converter;
    }

    @Bean
    JwtDecoder jwtDecoder() {
        SecretKeySpec secretKeySpec = new SecretKeySpec(SIGNER_KEY.getBytes(), "HS512");
        return NimbusJwtDecoder.withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
