package dev.send.api.analytics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class AnalyticsProxyService {
    private static final Set<String> SCRIPT_RESPONSE_HEADERS = Set.of(
            HttpHeaders.CACHE_CONTROL,
            HttpHeaders.ETAG,
            HttpHeaders.LAST_MODIFIED,
            HttpHeaders.CONTENT_TYPE);

    private final RestTemplate analyticsRestTemplate;
    private final UmamiAnalyticsProperties analyticsProperties;

    public AnalyticsProxyService(
            RestTemplate analyticsRestTemplate,
            UmamiAnalyticsProperties analyticsProperties) {
        this.analyticsRestTemplate = analyticsRestTemplate;
        this.analyticsProperties = analyticsProperties;
    }

    public AnalyticsBootstrapDto getBootstrapConfig() {
        if (!analyticsProperties.isEnabled()) {
            return new AnalyticsBootstrapDto(false, "", "", "");
        }

        return new AnalyticsBootstrapDto(
                true,
                UmamiAnalyticsProperties.SCRIPT_PATH,
                UmamiAnalyticsProperties.COLLECT_PATH,
                analyticsProperties.websiteId());
    }

    public java.util.Optional<String> analyticsDisabledReason() {
        return analyticsProperties.disabledReason();
    }

    public ProxyResponse fetchScript() {
        if (!analyticsProperties.isEnabled()) {
            return new ProxyResponse(HttpStatus.NOT_FOUND, new HttpHeaders(), new byte[0]);
        }

        return analyticsRestTemplate.execute(
                analyticsProperties.upstreamScriptUri(),
                HttpMethod.GET,
                request -> {
                    HttpHeaders headers = request.getHeaders();
                    headers.setConnection("close");
                    headers.setAccept(List.of(
                            MediaType.valueOf("application/javascript"),
                            MediaType.TEXT_PLAIN,
                            MediaType.ALL));
                },
                response -> extractResponse(response, SCRIPT_RESPONSE_HEADERS));
    }

    public ProxyResponse collect(
            byte[] requestBody,
            @Nullable String contentType,
            @Nullable String userAgent,
            @Nullable String clientIp) {
        if (!analyticsProperties.isEnabled()) {
            return new ProxyResponse(HttpStatus.NOT_FOUND, new HttpHeaders(), new byte[0]);
        }

        return analyticsRestTemplate.execute(
                analyticsProperties.upstreamCollectUri(),
                HttpMethod.POST,
                request -> {
                    HttpHeaders headers = request.getHeaders();
                    headers.setConnection("close");
                    if (contentType != null && !contentType.isBlank()) {
                        headers.set(HttpHeaders.CONTENT_TYPE, contentType);
                    }
                    if (userAgent != null && !userAgent.isBlank()) {
                        headers.set(HttpHeaders.USER_AGENT, userAgent);
                    }
                    if (clientIp != null && !clientIp.isBlank()) {
                        headers.set("X-Forwarded-For", clientIp);
                        headers.set("X-Real-IP", clientIp);
                    }
                    StreamUtils.copy(requestBody, request.getBody());
                },
                response -> extractResponse(response, Set.of(HttpHeaders.CONTENT_TYPE)));
    }

    private ProxyResponse extractResponse(
            org.springframework.http.client.ClientHttpResponse response,
            Set<String> allowedHeaders) throws IOException {
        HttpHeaders responseHeaders = new HttpHeaders();
        allowedHeaders.forEach(headerName -> {
            List<String> values = response.getHeaders().get(headerName);
            if (values != null && !values.isEmpty()) {
                responseHeaders.put(headerName, values);
            }
        });

        byte[] responseBody = StreamUtils.copyToByteArray(response.getBody());
        if (!responseHeaders.containsKey(HttpHeaders.CONTENT_TYPE) && responseBody.length > 0) {
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        }

        return new ProxyResponse(response.getStatusCode(), responseHeaders, responseBody);
    }

    public record AnalyticsBootstrapDto(
            boolean enabled,
            String scriptUrl,
            String collectUrl,
            String websiteId) {
    }

    public record ProxyResponse(
            HttpStatusCode statusCode,
            HttpHeaders headers,
            byte[] body) {
        public String bodyAsText() {
            return new String(body, StandardCharsets.UTF_8);
        }

        public HttpEntity<byte[]> toHttpEntity() {
            return new HttpEntity<>(body, headers);
        }
    }
}
