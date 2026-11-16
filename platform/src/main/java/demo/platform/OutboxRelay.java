package demo.platform;

import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "outbox.enabled", havingValue = "true")
public class OutboxRelay {
    private final JdbcTemplate db;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate tx;
    private final io.micrometer.core.instrument.MeterRegistry metrics;

    public OutboxRelay(JdbcTemplate db, KafkaTemplate<String, String> kafka, org.springframework.transaction.PlatformTransactionManager tm, io.micrometer.core.instrument.MeterRegistry metrics) {
        this.db = db;
        this.kafka = kafka;
        tx = new TransactionTemplate(tm);
        this.metrics = metrics;
    }

    @Scheduled(fixedDelay = 1500)
    public void relay() {
        try {
            tx.executeWithoutResult(s -> {
                for (var r : db.queryForList("select * from outbox where published=false order by created_at limit 20 for update skip locked")) {
                    try {
                        kafka.send("commerce.events.v1", r.get("aggregate_id").toString(), r.get("payload").toString()).get(3, java.util.concurrent.TimeUnit.SECONDS);
                        db.update("update outbox set published=true where id=?", r.get("id"));
                    } catch (Exception e) {
                        metrics.counter("outbox_publish_failures").increment();
                        throw new IllegalStateException("relay retry", e);
                    }
                }
            });
        } catch (RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Outbox pending: {}", e.getClass().getSimpleName());
        }
    }
}
