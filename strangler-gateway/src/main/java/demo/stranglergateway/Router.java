package demo.stranglergateway;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import demo.platform.Http;

import java.util.*;
import java.util.concurrent.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * A narrow strangler router: no arbitrary URLs, no write retry or fallback.
 */
@RestController
public class Router {
    private final Http http;
    private final io.micrometer.core.instrument.MeterRegistry metrics;
    private final ThreadPoolExecutor shadow = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(32), new ThreadPoolExecutor.AbortPolicy());

    public Router(Http http, io.micrometer.core.instrument.MeterRegistry metrics) {
        this.http = http;
        this.metrics = metrics;
    }

    @jakarta.annotation.PreDestroy
    public void stop() {
        shadow.shutdownNow();
    }

    @RequestMapping(value = "/api/{*path}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> route(HttpServletRequest req, @RequestBody(required = false) Object body) {
        String path = req.getRequestURI();
        if (!path.matches("/api/(products(/[0-9]+)?|orders(/[A-Za-z0-9-]+)?|customers/[0-9]+|report)"))
            return ResponseEntity.notFound().build();
        boolean get = req.getMethod().equals("GET"), eligible = get && path.matches("/api/products/[0-9]+");
        String legacy = http.url("LEGACY", 8081), catalog = http.url("CATALOG", 8083), dest = legacy;
        Map<?, ?> cfg = (Map<?, ?>) http.get(legacy + "/internal/routing");
        String mode = cfg.get("mode").toString();
        // Principal is authenticated; client-provided tenant headers cannot select privileged cohorts.
        int cohort = Math.floorMod(req.getUserPrincipal().getName().hashCode(), 100);
        if (eligible && (mode.equals("MODERN") || mode.equals("CANARY") && cohort < ((Number) cfg.get("percent")).intValue()))
            dest = catalog;
        metrics.counter("strangler_requests", "route", dest.equals(legacy) ? "legacy" : "catalog", "method", req.getMethod()).increment();
        Object result;
        try {
            result = get ? http.get(dest + path) : http.post(dest + path, body);
        } catch (org.springframework.web.client.HttpServerErrorException |
                 org.springframework.web.client.ResourceAccessException e) {
            if (!eligible || dest.equals(legacy)) throw e;
            // Legacy delegates after ownership cutover; this fallback does not read the stale table.
            metrics.counter("strangler_read_fallback").increment();
            result = http.get(legacy + path);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());
        }
        if (eligible && mode.equals("SHADOW")) {
            Object authoritative = result;
            try {
                shadow.execute(() -> {
                    try {
                        Object copy = http.get(catalog + path);
                        boolean match = Objects.equals(authoritative, copy);
                        metrics.counter("strangler_shadow", "result", match ? "match" : "mismatch").increment();
                    } catch (RuntimeException e) {
                        metrics.counter("strangler_shadow", "result", "error").increment();
                    }
                });
            } catch (RejectedExecutionException e) {
                metrics.counter("strangler_shadow", "result", "dropped").increment();
            }
        }
        return ResponseEntity.ok(result);
    }
}
