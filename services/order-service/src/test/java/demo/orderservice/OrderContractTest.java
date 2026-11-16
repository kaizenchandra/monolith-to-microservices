package demo.orderservice;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class OrderContractTest {
    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:17.6").withStartupTimeout(java.time.Duration.ofMinutes(5));
    @Autowired
    MockMvc mvc;
    @Autowired
    org.springframework.jdbc.core.JdbcTemplate db;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }

    @Test
    void zeroValueOrderRemainsValid() {
        db.update("insert into checkout(id,customer_id,product_id,quantity,amount_cents,state) values('free-order',1,2,1,0,'CONFIRMED')");
        org.junit.jupiter.api.Assertions.assertEquals(0L, db.queryForObject("select amount_cents from checkout where id='free-order'", Long.class));
    }

    @Test
    void missingOrderIs404() throws Exception {
        mvc.perform(get("/api/orders/unknown").with(user("reader").roles("READ"))).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void validationAndAuthorizationBeforeEffects() throws Exception {
        mvc.perform(post("/api/orders").contentType("application/json").content("{}").with(user("reader").roles("READ"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/orders").contentType("application/json").content("{}").with(user("admin").roles("WRITE"))).andExpect(status().isBadRequest());
    }
}
