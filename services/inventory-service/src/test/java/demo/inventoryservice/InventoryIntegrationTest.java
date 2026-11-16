package demo.inventoryservice;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import java.util.concurrent.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(properties = {"spring.task.scheduling.enabled=false"})
class InventoryIntegrationTest {
    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:17.6").withStartupTimeout(java.time.Duration.ofMinutes(5));
    @Autowired
    InventoryApi api;
    @Autowired
    JdbcTemplate db;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }

    @BeforeEach
    void seed() {
        db.update("delete from outbox");
        db.update("delete from reservations");
        db.update("delete from stock");
        db.update("update ownership set state='MODERN'");
        db.update("insert into stock values(1,1,1)");
    }

    @Test
    void lastUnitAndDuplicateIdentity() throws Exception {
        try (var pool = Executors.newFixedThreadPool(8)) {
            var jobs = new ArrayList<Callable<Boolean>>();
            for (int i = 0; i < 16; i++) {
                final int id = i;
                jobs.add(() -> {
                    try {
                        api.reserve(new InventoryApi.Request("r-" + id, 1, 1));
                        return true;
                    } catch (IllegalStateException e) {
                        return false;
                    }
                });
            }
            int winners = 0;
            for (var f : pool.invokeAll(jobs)) if (f.get()) winners++;
            assertEquals(1, winners);
        }
        assertEquals(0, db.queryForObject("select available from stock where product_id=1", Integer.class));
        String id = db.queryForObject("select id from reservations", String.class);
        api.reserve(new InventoryApi.Request(id, 1, 1));
        assertEquals(1, db.queryForObject("select count(*) from outbox", Integer.class));
        api.release(new InventoryApi.Request(id, 1, 1));
        api.release(new InventoryApi.Request(id, 1, 1));
        assertEquals(1, db.queryForObject("select available from stock where product_id=1", Integer.class));
    }

    @Test
    void fenceBlocksWritesAndRollbackHasNoOutbox() {
        db.update("update ownership set state='FROZEN'");
        assertThrows(IllegalStateException.class, () -> api.reserve(new InventoryApi.Request("fenced", 1, 1)));
        assertEquals(0, db.queryForObject("select count(*) from reservations", Integer.class));
    }
}
