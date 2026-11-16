package demo.catalogservice;

import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@RestController
public class CatalogApi {
    private final JdbcTemplate db;

    public CatalogApi(JdbcTemplate db) {
        this.db = db;
    }

    Product map(java.sql.ResultSet r) throws java.sql.SQLException {
        return new Product(r.getLong("id"), r.getString("name"), r.getLong("price_cents"), r.getLong("version"), r.getBoolean("deleted"));
    }

    @GetMapping("/api/products/{id}")
    public Product get(@PathVariable long id) {
        return db.query("select * from products where id=? and not deleted", (r, n) -> map(r), id).stream().findFirst().orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
    }

    @PostMapping("/api/products")
    @Transactional
    public Product put(@RequestBody Product p) {
        if (p.id < 1 || p.name == null || p.name.isBlank() || p.price_cents < 0)
            throw new IllegalArgumentException("invalid product");
        if (!"MODERN".equals(db.queryForObject("select state from ownership where capability='catalog' for share", String.class)))
            throw new IllegalStateException("catalog writer fenced");
        int n = db.update("update products set name=?,price_cents=?,version=version+1,deleted=? where id=? and version=?", p.name, p.price_cents, p.deleted, p.id, p.version);
        if (n != 1) throw new IllegalStateException("version conflict");
        Product result = db.queryForObject("select * from products where id=?", (r, i) -> map(r), p.id);
        try {
            String payload = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of("eventId", UUID.randomUUID().toString(), "schemaVersion", 1, "type", "ProductUpdated", "product", result));
            db.update("insert into outbox(id,aggregate_id,payload) values(?,?,?)", UUID.randomUUID(), "" + p.id, payload);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    @PostMapping("/internal/replicate")
    @Transactional
    public void replicate(@RequestBody List<Product> rows) {
        if (!"FROZEN".equals(db.queryForObject("select state from ownership where capability='catalog' for share", String.class)))
            throw new IllegalStateException("replication disabled on authority");
        for (var p : rows)
            db.update("insert into products values(?,?,?,?,?) on conflict(id) do update set name=excluded.name,price_cents=excluded.price_cents,version=excluded.version,deleted=excluded.deleted where products.version<excluded.version", p.id, p.name, p.price_cents, p.version, p.deleted);
    }

    public record Product(long id, String name, long price_cents, long version, boolean deleted) {
    }
}
