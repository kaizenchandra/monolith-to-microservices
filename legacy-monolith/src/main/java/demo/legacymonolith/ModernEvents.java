package demo.legacymonolith;

import org.springframework.stereotype.Component;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

@Component
public class ModernEvents {
    private final JdbcTemplate db;
    private final ObjectMapper json;

    public ModernEvents(JdbcTemplate db, ObjectMapper json) {
        this.db = db;
        this.json = json;
    }

    @KafkaListener(topics = "commerce.events.v1", groupId = "legacy-projections-v1")
    @Transactional
    public void accept(String payload) throws Exception {
        var e = json.readTree(payload);
        if (e.path("schemaVersion").asInt() != 1) throw new IllegalArgumentException("unsupported event schema");
        if (!e.path("type").asText().equals("OrderConfirmed")) return;
        if (db.update("insert into inbox values(?) on conflict do nothing", UUID.fromString(e.path("eventId").asText())) == 0)
            return;
        String id = e.path("aggregateId").asText();
        db.update("insert into modern_orders values(?,?) on conflict do nothing", id, e.path("amountCents").asLong());
        db.update("insert into shipments values(?,'PENDING') on conflict do nothing", id);
        db.update("insert into notifications values(?,'Order received') on conflict do nothing", id);
    }
}
