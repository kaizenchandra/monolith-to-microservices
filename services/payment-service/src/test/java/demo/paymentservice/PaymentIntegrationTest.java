package demo.paymentservice;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class PaymentIntegrationTest {
    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:17.6").withStartupTimeout(java.time.Duration.ofMinutes(5));
    @Autowired
    org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired
    PaymentApi api;
    @Autowired
    JdbcTemplate db;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }

    @Test
    void missingAmountIsNotAZeroAuthorization() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/internal/authorize").contentType("application/json").content("{\"id\":\"missing-amount\"}").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("service").roles("SERVICE"))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void zeroAuthorizationIsIdempotentAndConflictingAmountIsRejected() {
        var request = new PaymentApi.Authorization("free-order", 0);
        assertEquals("AUTHORIZED", api.authorize(request).get("state"));
        api.authorize(request);
        assertThrows(IllegalStateException.class, () -> api.authorize(new PaymentApi.Authorization("free-order", 1)));
        assertEquals(1, db.queryForObject("select count(*) from authorizations", Integer.class));
        assertEquals(1, db.queryForObject("select count(*) from outbox", Integer.class));
        api.cancel(request);
        api.cancel(request);
        assertThrows(IllegalStateException.class, () -> api.authorize(request));
    }
}
