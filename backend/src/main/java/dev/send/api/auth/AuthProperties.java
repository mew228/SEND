package dev.send.api.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.supabase")
public record AuthProperties(
        String issuerUri,
        String jwkSetUri,
        String jwtSecret,
        String audience) {}
