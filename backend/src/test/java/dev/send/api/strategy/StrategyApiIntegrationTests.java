package dev.send.api.strategy;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.send.api.strategy.application.StrategySimulationBounds;
import dev.send.api.strategy.application.StrategySimulationBoundsService;
import dev.send.api.strategy.domain.StrategyDocument;
import dev.send.api.worker.infra.ocaml.OcamlExecutionResponse;
import dev.send.api.worker.infra.ocaml.OcamlWorkerClient;

@SpringBootTest
@AutoConfigureMockMvc
class StrategyApiIntegrationTests {
  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private OcamlWorkerClient ocamlWorkerClient;

  @MockBean private StrategySimulationBoundsService strategySimulationBoundsService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUpJwtDecoder() {
        when(jwtDecoder.decode("valid-user-1")).thenReturn(validJwt("supabase-user-1", "user1@example.com"));
        when(jwtDecoder.decode("valid-user-2")).thenReturn(validJwt("supabase-user-2", "user2@example.com"));
        doThrow(new BadJwtException("Token expired.")).when(jwtDecoder).decode("expired-token");
        doThrow(new BadJwtException("Token issuer is invalid.")).when(jwtDecoder).decode("wrong-issuer-token");
        doThrow(new BadJwtException("Token audience is invalid.")).when(jwtDecoder).decode("wrong-audience-token");
    }

    @Test
    void listsBundledStrategiesAnonymouslyAndIncludesOwnedStrategiesForSignedInUsers() throws Exception {
        String strategyId = createUserStrategy("valid-user-1", "Momentum Draft");

        mockMvc.perform(get("/api/strategies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'logicex')]").exists())
                .andExpect(jsonPath("$[?(@.id == 'aapl_buy_sell_template')]").exists())
                .andExpect(jsonPath("$[?(@.id == '" + strategyId + "')]").doesNotExist());

        mockMvc.perform(get("/api/strategies")
                        .header("Authorization", "Bearer valid-user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + strategyId + "' && @.kind == 'user')]").exists());
    }

    @Test
    void createsFetchesAndUpdatesOwnedStrategies() throws Exception {
        String strategyId = createUserStrategy("valid-user-1", "Momentum Draft");

        mockMvc.perform(get("/api/strategies/" + strategyId)
                        .header("Authorization", "Bearer valid-user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(strategyId))
                .andExpect(jsonPath("$.name").value("Momentum Draft"))
                .andExpect(jsonPath("$.kind").value("user"))
                .andExpect(jsonPath("$.edges[1].targetHandle").value("in:1"));

        mockMvc.perform(put("/api/strategies/" + strategyId)
                        .header("Authorization", "Bearer valid-user-1")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Momentum Draft",
                                  "nodes": [
                                    {
                                      "id": "a",
                                      "type": "const_number",
                                      "position": { "x": 0, "y": 0 },
                                      "data": { "value": 5 }
                                    }
                                  ],
                                  "edges": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(strategyId))
                .andExpect(jsonPath("$.name").value("Updated Momentum Draft"))
                .andExpect(jsonPath("$.nodes[0].data.value").value(5));
    }

    @Test
    void bundledTemplatesRemainPublicButUserStrategiesAreOwnerOnly() throws Exception {
        String strategyId = createUserStrategy("valid-user-1", "Private Strategy");

        mockMvc.perform(get("/api/strategies/logicex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("logicex"))
                .andExpect(jsonPath("$.kind").value("template"));

        mockMvc.perform(get("/api/strategies/" + strategyId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/strategies/" + strategyId)
                        .header("Authorization", "Bearer valid-user-2"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/strategies/" + strategyId)
                        .header("Authorization", "Bearer valid-user-2")
                        .contentType(APPLICATION_JSON)
                        .content(strategyUpsertPayload("Blocked Update")))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsProtectedStrategyWritesWithoutAValidBearerToken() throws Exception {
        mockMvc.perform(post("/api/strategies")
                        .contentType(APPLICATION_JSON)
                        .content(strategyUpsertPayload("Missing Auth")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/strategies")
                        .header("Authorization", "Bearer expired-token")
                        .contentType(APPLICATION_JSON)
                        .content(strategyUpsertPayload("Expired")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/strategies")
                        .header("Authorization", "Bearer wrong-issuer-token")
                        .contentType(APPLICATION_JSON)
                        .content(strategyUpsertPayload("Wrong Issuer")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/strategies")
                        .header("Authorization", "Bearer wrong-audience-token")
                        .contentType(APPLICATION_JSON)
                        .content(strategyUpsertPayload("Wrong Audience")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsOversizedStrategyNamesBeforePersistence() throws Exception {
        mockMvc.perform(post("/api/strategies")
                        .header("Authorization", "Bearer valid-user-1")
                        .contentType(APPLICATION_JSON)
                        .content(strategyUpsertPayload("A".repeat(121))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("strategy_validation_failed"))
                .andExpect(jsonPath("$.message").value("Strategy names are limited to 120 characters."));
    }

    @Test
    void testsCurrentGraphThroughOcamlWorkerAndReturnsFlatNodeResults() throws Exception {
        ObjectNode result = objectMapper.createObjectNode();
        result.putObject("c").put("sum", 5);

        when(ocamlWorkerClient.executeGraph(any(StrategyDocument.class)))
                .thenReturn(new OcamlExecutionResponse("ok", "execute_graph", result, null, null, java.util.List.of()));

        mockMvc.perform(post("/api/strategies/test")
                        .contentType(APPLICATION_JSON)
                        .content(strategyDocumentPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.c.sum").value(5));
    }

    @Test
    void rejectsNestedNodeDataBeforeWorkerExecution() throws Exception {
        mockMvc.perform(post("/api/strategies/test")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "draft",
                                  "nodes": [
                                    {
                                      "id": "a",
                                      "type": "const_number",
                                      "position": { "x": 0, "y": 0 },
                                      "data": { "value": { "nested": true } }
                                    }
                                  ],
                                  "edges": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("strategy_validation_failed"))
                .andExpect(jsonPath("$.message")
                        .value("Data field 'value' for node a must be a scalar value."));
    }

    @Test
    void returnsOcamlExecutionErrorsWithSharedApiErrorShape() throws Exception {
        when(ocamlWorkerClient.executeGraph(any(StrategyDocument.class)))
                .thenReturn(new OcamlExecutionResponse(
                        "error",
                        "execute_graph",
                        null,
                        "graph_validation_failed",
                        "Graph validation failed.",
                        java.util.List.of("Missing source node: a")));

        mockMvc.perform(post("/api/strategies/test")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "draft",
                                  "nodes": [],
                                  "edges": []
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("graph_validation_failed"))
                .andExpect(jsonPath("$.message").value("Graph validation failed."))
                .andExpect(jsonPath("$.details[0]").value("Missing source node: a"));
    }

    @Test
    void simulatesGraphThroughOcamlWorkerAndReturnsStructuredSimulationResult() throws Exception {
        ObjectNode result = objectMapper.createObjectNode();
        result.putObject("summary")
                .put("executedDays", 2)
                .put("finalEquity", 1035.5);
        result.putObject("portfolio")
                .put("cash", 900.0);
        result.putObject("finalNodeValues")
                .putObject("buy-1")
                .put("executed", true);
        ArrayNode trace = result.putArray("trace");
        trace.addObject().put("date", "2024-01-02");
        result.putArray("warnings").add("Skipped sell on 2024-01-03.");

        when(ocamlWorkerClient.simulateGraph(
                        any(StrategyDocument.class),
                        any(dev.send.api.worker.application.StrategySimulationConfig.class)))
                .thenReturn(new OcamlExecutionResponse("ok", "simulate_graph", result, null, null, java.util.List.of()));

        mockMvc.perform(post("/api/strategies/simulate")
                        .header("X-Forwarded-For", "198.51.100.77")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": {
                                    "id": "draft",
                                    "nodes": [
                                      {
                                        "id": "a",
                                        "type": "const_bool",
                                        "position": { "x": 0, "y": 0 },
                                        "data": { "value": true }
                                      }
                                    ],
                                    "edges": []
                                  },
                                  "simulation": {
                                    "startDate": "2024-01-01",
                                    "endDate": "2024-01-31",
                                    "initialCash": 1000.0,
                                    "includeTrace": true
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.executedDays").value(2))
                .andExpect(jsonPath("$.portfolio.cash").value(900.0))
                .andExpect(jsonPath("$.finalNodeValues.buy-1.executed").value(true))
                .andExpect(jsonPath("$.trace[0].date").value("2024-01-02"))
                .andExpect(jsonPath("$.warnings[0]").value("Skipped sell on 2024-01-03."));
    }

    @Test
    void rejectsSimulationsThatFailServerSideBoundsValidation() throws Exception {
        doThrow(new dev.send.api.strategy.application.StrategyValidationException(
                "Simulations are limited to a maximum six-month range."))
                .when(strategySimulationBoundsService)
                .validateSimulationRequest(any(dev.send.api.worker.application.StrategySimulationConfig.class));

        mockMvc.perform(post("/api/strategies/simulate")
                        .header("X-Forwarded-For", "198.51.100.10")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": {
                                    "id": "draft",
                                    "nodes": [
                                      {
                                        "id": "a",
                                        "type": "const_bool",
                                        "position": { "x": 0, "y": 0 },
                                        "data": { "value": true }
                                      }
                                    ],
                                    "edges": []
                                  },
                                  "simulation": {
                                    "startDate": "2024-01-01",
                                    "endDate": "2024-09-01",
                                    "initialCash": 1000.0,
                                    "includeTrace": true
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("strategy_validation_failed"))
                .andExpect(jsonPath("$.message").value("Simulations are limited to a maximum six-month range."));
    }

    @Test
    void rejectsSimulationsWithOversizedInitialCash() throws Exception {
        mockMvc.perform(post("/api/strategies/simulate")
                        .header("X-Forwarded-For", "198.51.100.250")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "strategy": {
                                    "id": "draft",
                                    "nodes": [
                                      {
                                        "id": "a",
                                        "type": "const_bool",
                                        "position": { "x": 0, "y": 0 },
                                        "data": { "value": true }
                                      }
                                    ],
                                    "edges": []
                                  },
                                  "simulation": {
                                    "startDate": "2024-01-01",
                                    "endDate": "2024-01-31",
                                    "initialCash": 1000000001.0,
                                    "includeTrace": true
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("strategy_validation_failed"))
                .andExpect(jsonPath("$.message").value("Simulation initialCash must be less than or equal to 1000000000."));
    }

    @Test
    void ignoresSpoofedForwardedForHeadersFromUntrustedRemoteAddresses() throws Exception {
        ObjectNode result = objectMapper.createObjectNode();
        result.putObject("summary")
                .put("executedDays", 2)
                .put("finalEquity", 1035.5);
        result.putObject("portfolio")
                .put("cash", 900.0);
        result.putObject("finalNodeValues")
                .putObject("buy-1")
                .put("executed", true);
        result.putArray("trace").addObject().put("date", "2024-01-02");
        result.putArray("warnings");

        when(ocamlWorkerClient.simulateGraph(
                        any(StrategyDocument.class),
                        any(dev.send.api.worker.application.StrategySimulationConfig.class)))
                .thenReturn(new OcamlExecutionResponse("ok", "simulate_graph", result, null, null, java.util.List.of()));

        String payload = """
                {
                  "strategy": {
                    "id": "draft",
                    "nodes": [
                      {
                        "id": "a",
                        "type": "const_bool",
                        "position": { "x": 0, "y": 0 },
                        "data": { "value": true }
                      }
                    ],
                    "edges": []
                  },
                  "simulation": {
                    "startDate": "2024-01-01",
                    "endDate": "2024-01-31",
                    "initialCash": 1000.0,
                    "includeTrace": true
                  }
                }
                """;

        mockMvc.perform(post("/api/strategies/simulate")
                        .with(request -> {
                            request.setRemoteAddr("198.51.100.200");
                            return request;
                        })
                        .header("X-Forwarded-For", "203.0.113.10")
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/strategies/simulate")
                        .with(request -> {
                            request.setRemoteAddr("198.51.100.200");
                            return request;
                        })
                        .header("X-Forwarded-For", "203.0.113.11")
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("simulation_rate_limited"));
    }

    @Test
    void rateLimitsRepeatedSimulationRequestsForTheSameClient() throws Exception {
        ObjectNode result = objectMapper.createObjectNode();
        result.putObject("summary")
                .put("executedDays", 1)
                .put("finalEquity", 1000.0);
        result.putObject("portfolio")
                .put("cash", 1000.0);
        result.putObject("finalNodeValues");
        result.putArray("trace").addObject().put("date", "2024-01-02");
        result.putArray("warnings");

        when(ocamlWorkerClient.simulateGraph(
                        any(StrategyDocument.class),
                        any(dev.send.api.worker.application.StrategySimulationConfig.class)))
                .thenReturn(new OcamlExecutionResponse("ok", "simulate_graph", result, null, null, java.util.List.of()));

        String payload = """
                {
                  "strategy": {
                    "id": "draft",
                    "nodes": [
                      {
                        "id": "a",
                        "type": "const_bool",
                        "position": { "x": 0, "y": 0 },
                        "data": { "value": true }
                      }
                    ],
                    "edges": []
                  },
                  "simulation": {
                    "startDate": "2024-01-01",
                    "endDate": "2024-01-31",
                    "initialCash": 1000.0,
                    "includeTrace": true
                  }
                }
                """;

        mockMvc.perform(post("/api/strategies/simulate")
                        .header("X-Forwarded-For", "203.0.113.42")
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/strategies/simulate")
                        .header("X-Forwarded-For", "203.0.113.42")
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("simulation_rate_limited"))
                .andExpect(jsonPath("$.message").value("Strategy simulations are limited to one run every 5 seconds."))
                .andExpect(jsonPath("$.details[0]").value(startsWith("retryAfterMs=")));
    }

    @Test
    void returnsGlobalSimulationBoundsForSandboxStartup() throws Exception {
        when(strategySimulationBoundsService.getSimulationBounds())
                .thenReturn(new StrategySimulationBounds(true, "2020-04-27", "2025-03-28"));

        mockMvc.perform(get("/api/strategies/simulation-bounds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPriceData").value(true))
                .andExpect(jsonPath("$.earliestPriceDate").value("2020-04-27"))
                .andExpect(jsonPath("$.latestPriceDate").value("2025-03-28"));
    }

    private String createUserStrategy(String bearerToken, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/strategies")
                        .header("Authorization", "Bearer " + bearerToken)
                        .contentType(APPLICATION_JSON)
                        .content(strategyUpsertPayload(name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.kind").value("user"))
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        return payload.path("id").asText();
    }

    private String strategyUpsertPayload(String name) {
        return """
                {
                  "name": "%s",
                  "nodes": [
                    {
                      "id": "a",
                      "type": "const_number",
                      "position": { "x": 0, "y": 0 },
                      "data": { "value": 2 }
                    },
                    {
                      "id": "b",
                      "type": "const_number",
                      "position": { "x": 0, "y": 120 },
                      "data": { "value": 3 }
                    },
                    {
                      "id": "c",
                      "type": "add",
                      "position": { "x": 180, "y": 60 },
                      "data": {}
                    }
                  ],
                  "edges": [
                    {
                      "id": "e-1",
                      "source": "a",
                      "target": "c",
                      "sourceHandle": "out:0",
                      "targetHandle": "in:0"
                    },
                    {
                      "id": "e-2",
                      "source": "b",
                      "target": "c",
                      "sourceHandle": "out:0",
                      "targetHandle": "in:1"
                    }
                  ]
                }
                """.formatted(name);
    }

    private String strategyDocumentPayload() {
        return """
                {
                  "id": "draft",
                  "nodes": [
                    {
                      "id": "a",
                      "type": "const_number",
                      "position": { "x": 0, "y": 0 },
                      "data": { "value": 2 }
                    },
                    {
                      "id": "b",
                      "type": "const_number",
                      "position": { "x": 0, "y": 120 },
                      "data": { "value": 3 }
                    },
                    {
                      "id": "c",
                      "type": "add",
                      "position": { "x": 180, "y": 60 },
                      "data": {}
                    }
                  ],
                  "edges": [
                    {
                      "id": "e-1",
                      "source": "a",
                      "target": "c",
                      "sourceHandle": "out:0",
                      "targetHandle": "in:0"
                    },
                    {
                      "id": "e-2",
                      "source": "b",
                      "target": "c",
                      "sourceHandle": "out:0",
                      "targetHandle": "in:1"
                    }
                  ]
                }
                """;
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
}
