package demo.legacymonolith;

import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import demo.platform.Http;

import java.util.*;

@RestController
public class LegacyApi {
    private final JdbcTemplate db;
    private final Http http;
    private final org.springframework.transaction.support.TransactionTemplate tx;

    public LegacyApi(JdbcTemplate db, Http http, org.springframework.transaction.support.TransactionTemplate tx) {
        this.db = db;
        this.http = http;
        this.tx = tx;
    }

    String owner(String cap) {
        return db.queryForObject("select state from ownership where capability=? for share", String.class, cap);
    }

    @GetMapping("/api/products/{id}")
    @Transactional
    public Object product(@PathVariable long id) {
        String s = owner("catalog");
        if (s.equals("MODERN")) return http.get(http.url("CATALOG", 8083) + "/api/products/" + id);
        if (s.equals("FROZEN"))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "catalog transfer in progress");
        var rows = db.queryForList("select * from products where id=? and not deleted", id);
        if (rows.isEmpty())
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        return rows.getFirst();
    }

    @PostMapping("/api/products")
    @Transactional
    public Object product(@RequestBody Product p) {
        String s = owner("catalog");
        if (s.equals("MODERN")) return http.post(http.url("CATALOG", 8083) + "/api/products", p);
        if (!s.equals("LEGACY")) throw new IllegalStateException("catalog fenced");
        if (p.id < 1 || p.name == null || p.name.isBlank() || p.price_cents < 0)
            throw new IllegalArgumentException("invalid product");
        if (db.update("update products set name=?,price_cents=?,version=version+1,deleted=? where id=? and version=?", p.name, p.price_cents, p.deleted, p.id, p.version) != 1)
            throw new IllegalStateException("version conflict");
        return db.queryForMap("select * from products where id=?", p.id);
    }

    @PostMapping("/api/orders")
    public Object checkout(@RequestBody Checkout r) {
        Object result = tx.execute(status -> checkoutTx(r));
        // Release the source connection before the target calls back for customer/catalog.
        return result == Forward.ORDER ? http.post(http.url("ORDER", 8085) + "/api/orders", r) : result;
    }

    private Object checkoutTx(Checkout r) {
        if (r.id == null || !r.id.matches("[A-Za-z0-9-]{1,100}") || r.quantity <= 0 || r.quantity > 1000 || r.customer_id < 1)
            throw new IllegalArgumentException("invalid checkout");
        String s = owner("checkout");
        var existing = db.queryForList("select * from orders where id=?", r.id);
        if (!existing.isEmpty()) {
            var o = existing.getFirst();
            if (((Number) o.get("customer_id")).longValue() != r.customer_id || ((Number) o.get("product_id")).longValue() != r.product_id || ((Number) o.get("quantity")).intValue() != r.quantity)
                throw new IllegalStateException("idempotency payload conflict");
            return o;
        }
        if (s.equals("MODERN")) return Forward.ORDER;
        if (!s.equals("LEGACY")) throw new IllegalStateException("checkout fenced");
        if (!owner("inventory").equals("LEGACY"))
            throw new IllegalStateException("pause checkout until workflow cutover");
        // Serialize operation IDs even before insert; hash collisions only reduce concurrency.
        db.queryForList("select pg_advisory_xact_lock(hashtext(?))", r.id);
        var prior = db.queryForList("select * from orders where id=?", r.id);
        if (!prior.isEmpty()) {
            var o = prior.getFirst();
            if (((Number) o.get("customer_id")).longValue() != r.customer_id || ((Number) o.get("product_id")).longValue() != r.product_id || ((Number) o.get("quantity")).intValue() != r.quantity)
                throw new IllegalStateException("idempotency payload conflict");
            return o;
        }
        var p = (Map<?, ?>) product(r.product_id);
        long amount = Math.multiplyExact(((Number) p.get("price_cents")).longValue(), r.quantity);
        if (db.update("update stock set available=available-?,version=version+1 where product_id=? and available>=?", r.quantity, r.product_id, r.quantity) != 1)
            throw new IllegalStateException("insufficient stock");
        db.update("insert into orders values(?,?,?,?,?,'CONFIRMED')", r.id, r.customer_id, r.product_id, r.quantity, amount);
        db.update("insert into payments values(?,?,'AUTHORIZED')", r.id, amount);
        db.update("insert into shipments values(?,'PENDING')", r.id);
        db.update("insert into notifications values(?,'Order received')", r.id);
        return order(r.id);
    }

    @GetMapping("/api/orders/{id}")
    public Object order(@PathVariable String id) {
        var old = db.queryForList("select * from orders where id=?", id);
        if (!old.isEmpty()) return old.getFirst();
        return http.get(http.url("ORDER", 8085) + "/api/orders/" + id);
    }

    @GetMapping("/api/customers/{id}")
    public Object customer(@PathVariable long id) {
        return db.queryForMap("select * from customers where id=?", id);
    }

    @GetMapping("/api/report")
    public Object report() {
        return db.queryForMap("select count(*) as orders,coalesce(sum(amount_cents),0) as amount_cents from (select amount_cents from orders union all select amount_cents from modern_orders) all_orders");
    }

    @GetMapping("/internal/routing")
    public Object routing() {
        return db.queryForMap("select * from routing where id=1");
    }

    @PostMapping("/internal/admin/routing")
    public void routing(@RequestBody Routing r) {
        db.update("update routing set mode=?,percent=? where id=1", r.mode, r.percent);
    }

    private enum Forward {ORDER}

    public record Product(long id, String name, long price_cents, long version, boolean deleted) {
    }

    public record Checkout(String id, long customer_id, long product_id, int quantity) {
    }

    public record Routing(String mode, int percent) {
    }
}
