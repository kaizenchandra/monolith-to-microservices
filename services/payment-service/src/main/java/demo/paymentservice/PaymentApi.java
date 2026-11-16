package demo.paymentservice;

import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@RestController
public class PaymentApi {
    private final JdbcTemplate db;

    public PaymentApi(JdbcTemplate db) {
        this.db = db;
    }

    @PostMapping("/internal/authorize")
    @Transactional
    public Map<String, Object> authorize(@RequestBody Authorization a) {
        if (a.id == null || !a.id.matches("[a-zA-Z0-9-]{1,100}") || a.amount_cents < 0)
            throw new IllegalArgumentException("invalid payment");
        int inserted = db.update("insert into authorizations values(?,?,'AUTHORIZED') on conflict(id) do nothing", a.id, a.amount_cents);
        if (inserted == 1) {
            var eid = UUID.randomUUID();
            db.update("insert into outbox(id,aggregate_id,payload) values(?,?,?)", eid, a.id, "{\"eventId\":\"" + eid + "\",\"aggregateId\":\"" + a.id + "\",\"type\":\"PaymentAuthorized\",\"schemaVersion\":1}");
        }
        var row = db.queryForMap("select * from authorizations where id=? for update", a.id);
        if (((Number) row.get("amount_cents")).longValue() != a.amount_cents || !row.get("state").equals("AUTHORIZED"))
            throw new IllegalStateException("payment key conflict");
        return row;
    }

    @PostMapping("/internal/void")
    @Transactional
    public Map<String, String> cancel(@RequestBody Authorization a) {
        db.update("update authorizations set state='VOIDED' where id=?", a.id);
        return Map.of("state", "VOIDED");
    }

    public record Authorization(String id, long amount_cents) {
    }
}
