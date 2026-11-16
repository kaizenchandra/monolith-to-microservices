package demo.orderservice;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import demo.platform.Http;

import java.util.*;

@Component
public class SagaWorker {
    private final JdbcTemplate db;
    private final Http http;
    private final TransactionTemplate tx;

    public SagaWorker(JdbcTemplate db, Http http, org.springframework.transaction.PlatformTransactionManager tm) {
        this.db = db;
        this.http = http;
        tx = new TransactionTemplate(tm);
    }

    @Scheduled(fixedDelay = 800)
    public void tick() {
        tx.executeWithoutResult(t -> {
            for (var r : db.queryForList("select * from checkout where state not in ('CONFIRMED','CANCELLED','MANUAL') order by updated_at limit 10 for update skip locked"))
                step(r);
        });
    }

    void step(Map<String, Object> r) {
        String id = r.get("id").toString(), s = r.get("state").toString();
        String next = s;
        var stock = Map.of("id", id, "product_id", r.get("product_id"), "quantity", r.get("quantity"));
        var pay = Map.of("id", id, "amount_cents", r.get("amount_cents"));
        try {
            switch (s) {
                case "NEW" -> {
                    http.post(http.url("INVENTORY", 8084) + "/internal/reserve", stock);
                    next = "RESERVED";
                }
                case "RESERVED" -> {
                    http.post(http.url("PAYMENT", 8086) + "/internal/authorize", pay);
                    next = "AUTHORIZED";
                }
                case "AUTHORIZED" -> {
                    next = "CONFIRMED";
                    var eid = UUID.randomUUID();
                    db.update("insert into outbox(id,aggregate_id,payload) values(?,?,?)", eid, id, "{\"eventId\":\"" + eid + "\",\"aggregateId\":\"" + id + "\",\"amountCents\":" + r.get("amount_cents") + ",\"type\":\"OrderConfirmed\",\"schemaVersion\":1}");
                }
                case "COMPENSATING" -> {
                    http.post(http.url("PAYMENT", 8086) + "/internal/void", pay);
                    http.post(http.url("INVENTORY", 8084) + "/internal/release", stock);
                    next = "CANCELLED";
                }
                default -> throw new IllegalStateException("unknown state");
            }
            db.update("update checkout set state=?,attempts=0,updated_at=now() where id=?", next, id);
        } catch (org.springframework.web.client.HttpClientErrorException.Conflict e) {
            db.update("update checkout set state='COMPENSATING',updated_at=now() where id=?", id);
        } catch (RuntimeException e) {
            int tries = ((Number) r.get("attempts")).intValue() + 1;
            db.update("update checkout set attempts=?,state=?,updated_at=now() where id=?", tries, tries >= 10 ? "MANUAL" : s, id);
        }
    }

    @org.springframework.web.bind.annotation.RestController
    public static class Recovery {
        private final JdbcTemplate db;

        public Recovery(JdbcTemplate db) {
            this.db = db;
        }

        @org.springframework.web.bind.annotation.PostMapping("/internal/admin/recover/{id}")
        public void recover(@org.springframework.web.bind.annotation.PathVariable String id, @org.springframework.web.bind.annotation.RequestParam String state) {
            if (!Set.of("NEW", "RESERVED", "AUTHORIZED", "COMPENSATING").contains(state))
                throw new IllegalArgumentException("invalid recovery state");
            db.update("update checkout set state=?,attempts=0 where id=? and state='MANUAL'", state, id);
        }
    }
}
