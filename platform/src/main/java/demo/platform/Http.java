package demo.platform;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;

@Component
public class Http {
    private final RestClient client;
    private final RestClient reads;

    public Http(org.springframework.core.env.Environment env, RestClient.Builder builder) {
        var transport = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        var f = new JdkClientHttpRequestFactory(transport);
        String app = env.getProperty("spring.application.name", "");
        int budget = app.equals("strangler-gateway") ? 20 : app.equals("legacy-monolith") ? 15 : 5;
        f.setReadTimeout(Duration.ofSeconds(budget));
        var b = builder.requestFactory(f);
        if (env.matchesProfiles("production"))
            b.defaultHeader("Authorization", "Bearer " + env.getRequiredProperty("SERVICE_TOKEN"));
        else b.defaultHeaders(h -> h.setBasicAuth("service", env.getProperty("DEMO_PASSWORD", "local-only")));
        b.requestInterceptor((request, body, execution) -> {
            String id = org.slf4j.MDC.get("correlationId");
            if (id != null) request.getHeaders().set("X-Correlation-ID", id);
            return execution.execute(request, body);
        });
        client = b.build();
        if (app.equals("legacy-monolith")) {
            var fast = new JdkClientHttpRequestFactory(transport);
            fast.setReadTimeout(Duration.ofSeconds(3));
            reads = b.clone().requestFactory(fast).build();
        } else reads = client;
    }

    public Object get(String url) {
        return reads.get().uri(url).retrieve().body(Object.class);
    }

    public Object post(String url, Object body) {
        return client.post().uri(url).body(body).retrieve().body(Object.class);
    }

    public String url(String name, int port) {
        return System.getenv().getOrDefault(name + "_URL", "http://localhost:" + port);
    }
}
