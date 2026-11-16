package demo.platform;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.*;

@Configuration
public class Security {
    @Bean
    @Profile("!production")
    UserDetailsService users(@Value("${DEMO_PASSWORD:local-only}") String password) {
        var enc = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return new InMemoryUserDetailsManager(User.withUsername("reader").password(enc.encode(password)).roles("READ").build(), User.withUsername("admin").password(enc.encode(password)).roles("READ", "WRITE", "ADMIN", "SERVICE").build(), User.withUsername("service").password(enc.encode(password)).roles("READ", "WRITE", "SERVICE").build());
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity h, org.springframework.core.env.Environment env) throws Exception {
        h.csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS));
        h.authorizeHttpRequests(a -> a.requestMatchers("/actuator/health/**").permitAll().requestMatchers("/actuator/prometheus").hasAnyAuthority("ROLE_SERVICE", "SCOPE_service").requestMatchers("/internal/admin/**").hasAnyAuthority("ROLE_ADMIN", "SCOPE_admin").requestMatchers("/internal/**").hasAnyAuthority("ROLE_SERVICE", "SCOPE_service").requestMatchers(org.springframework.http.HttpMethod.GET, "/api/**").hasAnyAuthority("ROLE_READ", "SCOPE_catalog.read").anyRequest().hasAnyAuthority("ROLE_WRITE", "SCOPE_commerce.write"));
        if (env.matchesProfiles("production")) h.oauth2ResourceServer(o -> o.jwt(j -> {
        }));
        else h.httpBasic(b -> {
        });
        return h.build();
    }

    @Bean
    @Profile("production")
    JwtDecoder decoder(@Value("${JWT_ISSUER}") String issuer, @Value("${JWT_AUDIENCE}") String audience) {
        var d = JwtDecoders.<NimbusJwtDecoder>fromIssuerLocation(issuer);
        d.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), jwt -> jwt.getAudience().contains(audience) ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"))));
        return d;
    }
}
