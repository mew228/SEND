package dev.send.api.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import dev.send.api.auth.AuthProperties;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/admin/**").denyAll()
                        .requestMatchers(HttpMethod.GET, "/api/analytics/config").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/analytics/script.js").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/analytics/collect").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/strategies").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/strategies/*").authenticated()
                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(AuthProperties authProperties) {
        if (authProperties.issuerUri() == null || authProperties.issuerUri().isBlank()) {
            throw new IllegalStateException("app.auth.supabase.issuer-uri must be configured.");
        }

        NimbusJwtDecoder jwtDecoder = buildJwtDecoder(authProperties);
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(authProperties.issuerUri()),
                new AudienceValidator(authProperties.audience()));
        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }

    private NimbusJwtDecoder buildJwtDecoder(AuthProperties authProperties) {
        if (authProperties.jwtSecret() != null && !authProperties.jwtSecret().isBlank()) {
            SecretKey secretKey = new SecretKeySpec(
                    authProperties.jwtSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            return NimbusJwtDecoder.withSecretKey(secretKey)
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build();
        }

        if (authProperties.jwkSetUri() == null || authProperties.jwkSetUri().isBlank()) {
            throw new IllegalStateException(
                    "One of app.auth.supabase.jwt-secret or app.auth.supabase.jwk-set-uri must be configured.");
        }

        return NimbusJwtDecoder.withJwkSetUri(authProperties.jwkSetUri())
                .jwsAlgorithms(algorithms -> {
                    algorithms.add(SignatureAlgorithm.ES256);
                    algorithms.add(SignatureAlgorithm.RS256);
                })
                .build();
    }

    private static final class AudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final String expectedAudience;

        private AudienceValidator(String expectedAudience) {
            this.expectedAudience = expectedAudience;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            if (expectedAudience == null || expectedAudience.isBlank()) {
                return OAuth2TokenValidatorResult.success();
            }
            List<String> audiences = token.getAudience();
            if (audiences != null && audiences.contains(expectedAudience)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Token audience is invalid.", null));
        }
    }
}
