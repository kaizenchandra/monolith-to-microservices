package demo.orderservice;

import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import demo.platform.Http;

import java.util.*;

@RestController
public class OrderApi {
    private final JdbcTemplate db;
    private final Http http;

    public OrderApi(JdbcTemplate db, Http http) {
        this.db = db;
        this.http = http;
    }

    @PostMapping("/api/orders")
    @Transactional
    public Map<String, Object> create(@RequestBody Checkout r) {
        if (r.id == null || !r.id.matches("[A-Za-z0-9-]{1,100}") || r.quantity <= 0 || r.quantity > 1000 || r.customer_id < 1)
            throw new IllegalArgumentException("invalid checkout");
        http.get(http.url("LEGACY", 8081) + "/api/customers/" + r.customer_id);
        Map<?, ?> product = (Map<?, ?>) http.get(http.url("LEGACY", 8081) + "/api/products/" + r.product_id);
        long total = Math.multiplyExact(((Number) product.get("price_cents")).longValue(), r.quantity);
        db.update("insert into checkout(id,customer_id,product_id,quantity,amount_cents,state) values(?,?,?,?,?,'NEW') on conflict(id) do nothing", r.id, r.customer_id, r.product_id, r.quantity, total);
        var row = db.queryForMap("select * from checkout where id=?", r.id);
        if (((Number) row.get("customer_id")).longValue() != r.customer_id || ((Number) row.get("product_id")).longValue() != r.product_id || ((Number) row.get("quantity")).intValue() != r.quantity)
            throw new IllegalStateException("idempotency payload conflict");
        return row;
    }

    @GetMapping("/api/orders/{id}")
    public Map<String, Object> get(@PathVariable String id) {
        return db.queryForMap("select * from checkout where id=?", id);
    }

    public record Checkout(String id, long customer_id, long product_id, int quantity) {
    }
}
