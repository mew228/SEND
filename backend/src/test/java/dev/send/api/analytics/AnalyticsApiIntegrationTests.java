package dev.send.api.analytics;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsApiIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate analyticsRestTemplate;

    @MockBean
    private JwtDecoder jwtDecoder;

    private MockRestServiceServer mockRestServiceServer;

    @BeforeEach
    void setUp() {
        mockRestServiceServer = MockRestServiceServer.bindTo(analyticsRestTemplate).ignoreExpectOrder(true).build();

        when(jwtDecoder.decode("valid-user-1")).thenReturn(validJwt("supabase-user-1", "user1@example.com"));
        doThrow(new BadJwtException("Token expired.")).when(jwtDecoder).decode("expired-token");
    }

    @Test
    void returnsBootstrapConfigForFrontend() throws Exception {
        mockMvc.perform(get("/api/analytics/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled", is(true)))
                .andExpect(jsonPath("$.scriptUrl", is("/api/analytics/script.js")))
                .andExpect(jsonPath("$.collectUrl", is("/api/analytics/collect")))
                .andExpect(jsonPath("$.websiteId", is("test-website-id")));
    }

    @Test
    void proxiesTrackerScriptAnonymously() throws Exception {
        mockRestServiceServer.expect(requestTo("http://umami.internal/script.js"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("console.log('umami')", MediaType.valueOf("application/javascript"))
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=600")
                        .header(HttpHeaders.ETAG, "\"tracker\""));

        mockMvc.perform(get("/api/analytics/script.js"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.CONTENT_TYPE, "application/javascript"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.CACHE_CONTROL, "public, max-age=600"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.ETAG, "\"tracker\""))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string("console.log('umami')"));
    }

    @Test
    void proxiesCollectRequestWithoutForwardingAuthOrCookies() throws Exception {
        String payload = "{\"type\":\"event\",\"payload\":{\"url\":\"/\"}}";

        mockRestServiceServer.expect(requestTo("http://umami.internal/api/analytics/collect"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.USER_AGENT, "Mozilla/5.0"))
                .andExpect(header("X-Forwarded-For", "198.51.100.24"))
                .andExpect(header("X-Real-IP", "198.51.100.24"))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andExpect(headerDoesNotExist(HttpHeaders.COOKIE))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content().json(payload))
                .andRespond(withSuccess("{\"sessionId\":\"abc\"}", APPLICATION_JSON));

        mockMvc.perform(post("/api/analytics/collect")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.10");
                            return request;
                        })
                        .header("X-Forwarded-For", "198.51.100.24")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-user-1")
                        .header(HttpHeaders.COOKIE, "session=secret")
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .json("{\"sessionId\":\"abc\"}"));
    }

    @Test
    void analyticsEndpointsRemainPublicWhileProtectedStrategyWritesStillRequireAuth() throws Exception {
        mockMvc.perform(get("/api/analytics/config"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/strategies")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Missing Auth",
                                  "nodes": [],
                                  "edges": []
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usesRemoteAddressWhenForwardedHeaderComesFromUntrustedClient() throws Exception {
        mockRestServiceServer.expect(requestTo("http://umami.internal/api/analytics/collect"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Forwarded-For", "198.51.100.200"))
                .andExpect(header("X-Real-IP", "198.51.100.200"))
                .andRespond(withSuccess("{}", APPLICATION_JSON));

        mockMvc.perform(post("/api/analytics/collect")
                        .with(request -> {
                            request.setRemoteAddr("198.51.100.200");
                            return request;
                        })
                        .header("X-Forwarded-For", "203.0.113.10")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                        .contentType(APPLICATION_JSON)
                        .content("{\"type\":\"event\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void returnsBadGatewayWhenUpstreamIsUnavailable() throws Exception {
        mockRestServiceServer.expect(requestTo("http://umami.internal/script.js"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    throw new ResourceAccessException("umami unavailable");
                });

        mockMvc.perform(get("/api/analytics/script.js"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code", is("analytics_proxy_failed")))
                .andExpect(jsonPath("$.message", is("umami unavailable")));
    }

    private Jwt validJwt(String subject, String email) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test-jwt-" + subject)
                .header("alg", "RS256")
                .claim("sub", subject)
                .claim("email", email)
                .claim("aud", java.util.List.of("authenticated"))
                .claim("iss", "https://test-project.supabase.co/auth/v1")
                .claim("role", "authenticated")
                .issuedAt(now.minusSeconds(60))
                .expiresAt(now.plusSeconds(3600))
                .build();
    }

    private static org.springframework.test.web.client.RequestMatcher headerDoesNotExist(String headerName) {
        return request -> org.assertj.core.api.Assertions.assertThat(request.getHeaders().containsKey(headerName))
                .as("header %s should not be forwarded", headerName)
                .isFalse();
    }
}
