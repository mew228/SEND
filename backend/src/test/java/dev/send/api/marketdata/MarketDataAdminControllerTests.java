package dev.send.api.marketdata;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.send.api.marketdata.application.MarketDataRefreshService;

@SpringBootTest
@AutoConfigureMockMvc
class MarketDataAdminControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketDataRefreshService marketDataRefreshService;

    @Test
    void deniesAllAdminReads() throws Exception {
        mockMvc.perform(get("/api/admin/market-data/tickers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deniesAllAdminWrites() throws Exception {
        mockMvc.perform(post("/api/admin/market-data/tickers?symbol=AAPL"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/admin/market-data/tickers/AAPL"))
                .andExpect(status().isUnauthorized());
    }
}
