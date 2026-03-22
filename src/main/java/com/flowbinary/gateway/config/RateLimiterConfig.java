package com.flowbinary.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

public class RateLimiterConfig {

    /**
     * Rate-limits by the caller's IP address.
     * Falls back to "unknown" when the remote address is unavailable (e.g. behind a proxy without
     * X-Forwarded-For). Replace with a user/JWT-based resolver if per-user limits are needed.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                .map(addr -> addr.getAddress().getHostAddress())
                .defaultIfEmpty("unknown");
    }
}
