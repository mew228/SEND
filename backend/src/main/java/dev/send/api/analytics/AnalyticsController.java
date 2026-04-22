package dev.send.api.analytics;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import dev.send.api.strategy.api.ApiErrorDto;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsProxyService analyticsProxyService;
    private final ClientAddressResolver clientAddressResolver;

    public AnalyticsController(
            AnalyticsProxyService analyticsProxyService,
            ClientAddressResolver clientAddressResolver) {
        this.analyticsProxyService = analyticsProxyService;
        this.clientAddressResolver = clientAddressResolver;
        analyticsProxyService.analyticsDisabledReason().ifPresent(log::warn);
    }

    @GetMapping("/config")
    public AnalyticsProxyService.AnalyticsBootstrapDto getConfig() {
        return analyticsProxyService.getBootstrapConfig();
    }

    @GetMapping("/script.js")
    public ResponseEntity<byte[]> getScript() {
        AnalyticsProxyService.ProxyResponse response = analyticsProxyService.fetchScript();
        return ResponseEntity.status(response.statusCode())
                .headers(response.headers())
                .body(response.body());
    }

    @PostMapping("/collect")
    public ResponseEntity<byte[]> collect(
            @RequestBody byte[] requestBody,
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest request) {
        AnalyticsProxyService.ProxyResponse response = analyticsProxyService.collect(
                requestBody,
                contentType,
                userAgent,
                clientAddressResolver.resolve(request));
        return ResponseEntity.status(response.statusCode())
                .headers(response.headers())
                .body(response.body());
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiErrorDto> handleAnalyticsProxyError(RestClientException exception) {
        String message = exception.getMessage();
        log.warn("Umami analytics proxy request failed: {}", message, exception);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiErrorDto(
                        "analytics_proxy_failed",
                        message == null ? "Analytics proxy request failed." : message,
                        List.of()));
    }
}
