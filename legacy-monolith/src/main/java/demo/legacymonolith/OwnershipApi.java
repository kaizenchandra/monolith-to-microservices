package demo.legacymonolith;

import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.*;

import java.util.*;

@RestController
public class OwnershipApi {
    private final JdbcTemplate db;

    public OwnershipApi(JdbcTemplate db) {
        this.db = db;
    }

    private void check(String cap) {
        if (!Set.of("catalog", "inventory", "checkout").contains(cap))
            throw new IllegalArgumentException("unknown capability");
    }

    @GetMapping("/internal/ownership/{cap}")
    public Map<String, Object> state(@PathVariable String cap) {
        check(cap);
        return db.queryForMap("select * from ownership where capability=?", cap);
    }

    @PostMapping("/internal/admin/ownership/{cap}")
    @Transactional
    public Map<String, Object> fence(@PathVariable String cap, @RequestBody Fence f) {
        check(cap);
        if (!Set.of("FROZEN", "LEGACY", "MODERN").contains(f.next)) throw new IllegalArgumentException("invalid state");
        if (!f.expected.equals("FROZEN") && !f.next.equals("FROZEN"))
            throw new IllegalArgumentException("must fence first");
        if (db.update("update ownership set state=?,epoch=epoch+1 where capability=? and state=? and epoch=?", f.next, cap, f.expected, f.epoch) != 1)
            throw new IllegalStateException("ownership epoch conflict");
        return state(cap);
    }

    @GetMapping("/internal/export/{cap}")
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Map<String, Object> export(@PathVariable String cap) {
        check(cap);
        var owner = db.queryForMap("select * from ownership where capability=? for share", cap);
        var result = new LinkedHashMap<String, Object>();
        result.put("owner", owner);
        if (cap.equals("catalog")) result.put("products", db.queryForList("select * from products order by id"));
        if (cap.equals("inventory")) {
            result.put("stock", db.queryForList("select * from stock order by product_id"));
            result.put("reservations", db.queryForList("select * from reservations order by id"));
        }
        return result;
    }

    @PostMapping("/internal/admin/import/{cap}")
    @Transactional
    public void load(@PathVariable String cap, @RequestBody Map<String, List<Map<String, Object>>> data) {
        check(cap);
        if (!"FROZEN".equals(db.queryForObject("select state from ownership where capability=? for update", String.class, cap)))
            throw new IllegalStateException("import requires fence");
        if (cap.equals("catalog")) {
            db.update("delete from products");
            for (var r : data.get("products"))
                db.update("insert into products values(?,?,?,?,?)", r.get("id"), r.get("name"), r.get("price_cents"), r.get("version"), r.get("deleted"));
        }
        if (cap.equals("inventory")) {
            db.update("delete from reservations");
            db.update("delete from stock");
            for (var r : data.get("stock"))
                db.update("insert into stock values(?,?,?)", r.get("product_id"), r.get("available"), r.get("version"));
            for (var r : data.get("reservations"))
                db.update("insert into reservations values(?,?,?,?)", r.get("id"), r.get("product_id"), r.get("quantity"), r.get("state"));
        }
    }

    public record Fence(String expected, String next, long epoch) {
    }
}
