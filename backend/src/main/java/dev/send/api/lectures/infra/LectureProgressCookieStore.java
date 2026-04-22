package dev.send.api.lectures.infra;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.send.api.lectures.domain.LectureModels.LectureProgress;

@Component
public class LectureProgressCookieStore {
    private static final String COOKIE_NAME = "send_lecture_progress";
    private static final Logger log = LoggerFactory.getLogger(LectureProgressCookieStore.class);

    private final ObjectMapper objectMapper;
    private final byte[] signingKey;

    public LectureProgressCookieStore(
            ObjectMapper objectMapper,
            @Value("${app.lectures.progress-cookie-secret:}") String configuredSigningSecret) {
        this.objectMapper = objectMapper;
        this.signingKey = resolveSigningKey(configuredSigningSecret);
    }

    public Map<String, LectureProgress> readProgressMap(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Map.of();
        }

        for (Cookie cookie : cookies) {
            if (!COOKIE_NAME.equals(cookie.getName()) || cookie.getValue() == null || cookie.getValue().isBlank()) {
                continue;
            }

            try {
                String encodedPayload = verifiedPayload(cookie.getValue());
                if (encodedPayload == null) {
                    return Map.of();
                }
                byte[] decoded = Base64.getUrlDecoder().decode(encodedPayload);
                Map<String, LectureProgress> parsed = objectMapper.readValue(
                        decoded,
                        new TypeReference<Map<String, LectureProgress>>() {});
                return Map.copyOf(parsed);
            } catch (Exception exception) {
                return Map.of();
            }
        }

        return Map.of();
    }

    public void writeProgressMap(HttpServletResponse response, Map<String, LectureProgress> progressMap) {
        try {
            String json = objectMapper.writeValueAsString(new HashMap<>(progressMap));
            String encodedPayload = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
            String signature = sign(encodedPayload);
            String encodedCookie = "v1." + encodedPayload + "." + signature;

            ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, encodedCookie)
                    .httpOnly(true)
                    .sameSite("Lax")
                    .path("/api/lectures")
                    .maxAge(Duration.ofDays(30))
                    .build();
            response.addHeader("Set-Cookie", cookie.toString());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to write lecture progress cookie.", exception);
        }
    }

    private byte[] resolveSigningKey(String configuredSigningSecret) {
        if (configuredSigningSecret != null && !configuredSigningSecret.isBlank()) {
            return configuredSigningSecret.getBytes(StandardCharsets.UTF_8);
        }

        byte[] generatedKey = new byte[32];
        new SecureRandom().nextBytes(generatedKey);
        log.warn("app.lectures.progress-cookie-secret is not configured; using an ephemeral signing key.");
        return generatedKey;
    }

    private String sign(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            byte[] signature = mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign lecture progress cookie.", exception);
        }
    }

    private @Nullable String verifiedPayload(String encodedCookie) {
        String[] parts = encodedCookie.split("\\.", 3);
        if (parts.length != 3 || !"v1".equals(parts[0])) {
            return null;
        }

        String expectedSignature = sign(parts[1]);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        return parts[1];
    }
}
