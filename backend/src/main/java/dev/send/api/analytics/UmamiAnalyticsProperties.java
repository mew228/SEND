package dev.send.api.analytics;

import java.net.URI;
import java.util.Optional;

import javax.annotation.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.util.UriComponentsBuilder;

@ConfigurationProperties(prefix = "app.analytics.umami")
public record UmamiAnalyticsProperties(
        String scriptUri,
        String websiteId) {
    public static final String CONFIG_PATH = "/api/analytics/config";
    public static final String SCRIPT_PATH = "/api/analytics/script.js";
    public static final String COLLECT_PATH = "/api/analytics/collect";

    public UmamiAnalyticsProperties {
        scriptUri = normalize(scriptUri);
        websiteId = normalize(websiteId);

        if (isEnabled()) {
            try {
                URI parsedScriptUri = URI.create(scriptUri);
                if (!parsedScriptUri.isAbsolute()) {
                    throw new IllegalStateException("app.analytics.umami.script-uri must be an absolute URI.");
                }
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("app.analytics.umami.script-uri must be a valid absolute URI.", exception);
            }
        }
    }

    public boolean isEnabled() {
        return scriptUri != null && websiteId != null;
    }

    public boolean isPartiallyConfigured() {
        return (scriptUri == null) != (websiteId == null);
    }

    public Optional<String> disabledReason() {
        if (isEnabled()) {
            return Optional.empty();
        }
        if (isPartiallyConfigured()) {
            return Optional.of("Umami analytics disabled: both app.analytics.umami.script-uri and app.analytics.umami.website-id must be set.");
        }
        return Optional.of("Umami analytics disabled: configuration not set.");
    }

    public URI upstreamScriptUri() {
        return URI.create(scriptUri);
    }

    public URI upstreamCollectUri() {
        return UriComponentsBuilder.fromUri(upstreamScriptUri())
                .replacePath(COLLECT_PATH)
                .replaceQuery(null)
                .fragment(null)
                .build(true)
                .toUri();
    }

    private static @Nullable String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
