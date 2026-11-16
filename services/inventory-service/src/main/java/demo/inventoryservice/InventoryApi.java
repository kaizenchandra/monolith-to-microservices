package demo.inventoryservice;

import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@RestController
public class InventoryApi {
    private final JdbcTemplate db;

    public InventoryApi(JdbcTemplate db) {
        this.db = db;
    }

    @PostMapping("/internal/reserve")
    @Transactional
    public Map<String, Object> reserve(@RequestBody Request r) {
        if (r.id == null || !r.id.matches("[A-Za-z0-9-]{1,100}") || r.quantity <= 0)
            throw new IllegalArgumentException("invalid reservation");
        if (!"MODERN".equals(db.queryForObject("select state from ownership where capability='inventory' for share", String.class)))
            throw new IllegalStateException("inventory fenced");
        // Stock row serializes same-product operations; reservation key serializes duplicate order requests.
        db.queryForList("select * from stock where product_id=? for update", r.product_id);
        var old = db.queryForList("select * from reservations where id=?", r.id);
        if (!old.isEmpty()) {
            var o = old.getFirst();
            if (((Number) o.get("product_id")).longValue() != r.product_id || ((Number) o.get("quantity")).intValue() != r.quantity)
                throw new IllegalStateException("idempotency payload conflict");
            if (!o.get("state").equals("RESERVED")) throw new IllegalStateException("reservation released");
            return o;
        }
        if (db.update("update stock set available=available-?,version=version+1 where product_id=? and available>=?", r.quantity, r.product_id, r.quantity) != 1)
            throw new IllegalStateException("insufficient stock");
        db.update("insert into reservations values(?,?,?,'RESERVED')", r.id, r.product_id, r.quantity);
        event(r.id, "InventoryReserved");
        return Map.of("id", r.id, "state", "RESERVED");
    }

    @PostMapping("/internal/release")
    @Transactional
    public Map<String, String> release(@RequestBody Request r) {
        if (!"MODERN".equals(db.queryForObject("select state from ownership where capability='inventory' for share", String.class)))
            throw new IllegalStateException("inventory fenced");
        var old = db.queryForList("select * from reservations where id=? for update", r.id);
        if (!old.isEmpty() && old.getFirst().get("state").equals("RESERVED")) {
            var o = old.getFirst();
            db.update("update stock set available=available+?,version=version+1 where product_id=?", o.get("quantity"), o.get("product_id"));
            db.update("update reservations set state='RELEASED' where id=?", r.id);
            event(r.id, "InventoryReleased");
        }
        return Map.of("state", "RELEASED");
    }

    void event(String id, String type) {
        var eid = UUID.randomUUID();
        db.update("insert into outbox(id,aggregate_id,payload) values(?,?,?)", eid, id, "{\"eventId\":\"" + eid + "\",\"aggregateId\":\"" + id + "\",\"schemaVersion\":1,\"type\":\"" + type + "\"}");
    }

    public record Request(String id, long product_id, int quantity) {
    }
}
