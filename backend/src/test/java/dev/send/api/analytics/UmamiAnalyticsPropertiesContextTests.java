package dev.send.api.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class UmamiAnalyticsPropertiesContextTests {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class));

    @Test
    void remainsDisabledWhenScriptUriIsMissing() {
        contextRunner
                .withPropertyValues("app.analytics.umami.website-id=test-website-id")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    UmamiAnalyticsProperties properties = context.getBean(UmamiAnalyticsProperties.class);
                    assertThat(properties.isEnabled()).isFalse();
                    assertThat(properties.isPartiallyConfigured()).isTrue();
                    assertThat(properties.disabledReason())
                            .contains("Umami analytics disabled: both app.analytics.umami.script-uri and app.analytics.umami.website-id must be set.");
                });
    }

    @Test
    void remainsDisabledWhenWebsiteIdIsMissing() {
        contextRunner
                .withPropertyValues("app.analytics.umami.script-uri=http://umami.internal/script.js")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    UmamiAnalyticsProperties properties = context.getBean(UmamiAnalyticsProperties.class);
                    assertThat(properties.isEnabled()).isFalse();
                    assertThat(properties.isPartiallyConfigured()).isTrue();
                    assertThat(properties.disabledReason())
                            .contains("Umami analytics disabled: both app.analytics.umami.script-uri and app.analytics.umami.website-id must be set.");
                });
    }

    @Test
    void remainsDisabledWhenAnalyticsConfigIsOmitted() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            UmamiAnalyticsProperties properties = context.getBean(UmamiAnalyticsProperties.class);
            assertThat(properties.isEnabled()).isFalse();
            assertThat(properties.isPartiallyConfigured()).isFalse();
            assertThat(properties.disabledReason())
                    .contains("Umami analytics disabled: configuration not set.");
        });
    }

    @Test
    void enablesAnalyticsWhenBothPropertiesAreSet() {
        contextRunner
                .withPropertyValues(
                        "app.analytics.umami.script-uri=http://umami.internal/script.js",
                        "app.analytics.umami.website-id=test-website-id")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    UmamiAnalyticsProperties properties = context.getBean(UmamiAnalyticsProperties.class);
                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.isPartiallyConfigured()).isFalse();
                    assertThat(properties.disabledReason()).isEmpty();
                });
    }

    @Configuration
    @EnableConfigurationProperties(UmamiAnalyticsProperties.class)
    static class TestConfig {
    }
}
