package com.flowbinary.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/actuator/health/**",
            "/actuator/info",
            "/platform/api/v1/tenants/resolve",
            "/platform/api/v1/lookups",
            "/platform/api/v1/lookups/**",
            "/platform/api/v1/plans",
            "/tenant/api/v1/email-verification/**",
            "/tenant/api/v1/account/signup",
            "/notifications/health"
    };

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // CORS config is handled by spring.cloud.gateway.server.webflux.globalcors
                .cors(Customizer.withDefaults())
                .authorizeExchange(auth -> auth
                        // Allow CORS preflight without auth check
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers(PUBLIC_PATHS).permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults()))
                .build();
    }
}
