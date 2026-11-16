package demo.legacymonolith;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import demo.platform.Http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(properties = {"spring.datasource.hikari.maximum-pool-size=2", "spring.datasource.hikari.connection-timeout=1000", "spring.kafka.listener.auto-startup=false"})
class LegacyIntegrationTest {
    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:17.6").withStartupTimeout(java.time.Duration.ofMinutes(5));
    @Autowired
    JdbcTemplate db;
    @Autowired
    LegacyApi api;
    @Autowired
    javax.sql.DataSource source;
    @org.springframework.boot.test.mock.mockito.MockBean
    Http http;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }

    @BeforeEach
    void owners() {
        var pool = (com.zaxxer.hikari.HikariDataSource) source;
        pool.setMinimumIdle(0);
        pool.setMaximumPoolSize(1);
        pool.getHikariPoolMXBean().softEvictConnections();
        db.update("update ownership set state='LEGACY'");
    }

    @Test
    void sourceConnectionIsReleasedBeforeTargetCallbacks() throws Exception {
        db.update("update ownership set state='MODERN' where capability='checkout'");
        when(http.url("ORDER", 8085)).thenReturn("http://order");
        when(http.post(eq("http://order/api/orders"), any())).thenAnswer(invocation -> {
            assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
            var worker = java.util.concurrent.Executors.newSingleThreadExecutor();
            try {
                return worker.submit(() -> {
                    assertEquals(1, db.queryForObject("select count(*) from customers", Integer.class));
                    return java.util.Map.of("state", "NEW");
                }).get(3, java.util.concurrent.TimeUnit.SECONDS);
            } finally {
                worker.shutdownNow();
            }
        });
        assertEquals("NEW", ((java.util.Map<?, ?>) api.checkout(new LegacyApi.Checkout("pool-proof", 1, 1, 1))).get("state"));
    }

    @Test
    void legacyForeignKeyFailureRollsBackStockDeduction() {
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> api.checkout(new LegacyApi.Checkout("fk-rollback", 999, 1, 1)));
        assertEquals(10, db.queryForObject("select available from stock where product_id=1", Integer.class));
        assertEquals(0, db.queryForObject("select count(*) from orders", Integer.class));
    }
}
