package demo.catalogservice;

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
class CatalogIntegrationTest {
    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:17.6").withStartupTimeout(java.time.Duration.ofMinutes(5));
    @Autowired
    org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired
    CatalogApi api;
    @Autowired
    JdbcTemplate db;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }

    @Test
    void missingPriceCannotSilentlyBecomeAFreeProduct() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/products").contentType("application/json").content("{\"id\":1,\"name\":\"Cable\",\"version\":1,\"deleted\":false}").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("WRITE"))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void optimisticWriteAndOutboxAtomicity() {
        db.update("update ownership set state='MODERN'");
        db.update("insert into products values(1,'Cable',100,1,false)");
        var p = new CatalogApi.Product(1, "Cable", 125, 1, false);
        assertEquals(2, api.put(p).version());
        assertThrows(IllegalStateException.class, () -> api.put(p));
        assertEquals(1, db.queryForObject("select count(*) from outbox", Integer.class));
        db.update("update ownership set state='FROZEN'");
        assertThrows(IllegalStateException.class, () -> api.put(new CatalogApi.Product(1, "Cable", 150, 2, false)));
        assertEquals(125, api.get(1).price_cents());
    }

    @Test
    void duplicateAndOutOfOrderSnapshots() {
        db.update("update ownership set state='FROZEN'");
        var latest = new CatalogApi.Product(2, "Mouse", 200, 4, false);
        api.replicate(java.util.List.of(latest));
        api.replicate(java.util.List.of(latest));
        api.replicate(java.util.List.of(new CatalogApi.Product(2, "Old", 100, 2, false)));
        assertEquals(latest, api.get(2));
    }
}
