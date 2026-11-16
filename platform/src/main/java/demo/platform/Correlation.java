package demo.platform;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.UUID;

@org.springframework.stereotype.Component
public class Correlation extends org.springframework.web.filter.OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain c) throws IOException, ServletException {
        String id = r.getHeader("X-Correlation-ID");
        if (id == null || !id.matches("[A-Za-z0-9-]{1,64}")) id = UUID.randomUUID().toString();
        s.setHeader("X-Correlation-ID", id);
        org.slf4j.MDC.put("correlationId", id);
        try {
            c.doFilter(r, s);
        } finally {
            org.slf4j.MDC.remove("correlationId");
        }
    }
}
