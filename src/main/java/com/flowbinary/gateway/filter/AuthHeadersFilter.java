package com.flowbinary.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class AuthHeadersFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthHeadersFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .flatMap(jwt -> chain.filter(exchange.mutate()
                        .request(buildRequest(exchange, jwt))
                        .build()))
                .switchIfEmpty(chain.filter(exchange));
    }

    private ServerHttpRequest buildRequest(ServerWebExchange exchange, Jwt jwt) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();

        setIfPresent(builder, "X-User-Id",    jwt.getSubject());
        setIfPresent(builder, "X-User-Name",  jwt.getClaimAsString("preferred_username"));
        setIfPresent(builder, "X-User-Email", jwt.getClaimAsString("email"));

        Object orgClaim = jwt.getClaim("organization");
        if (orgClaim instanceof Map<?, ?> orgMap && !orgMap.isEmpty()) {
            String tenantAlias = orgMap.keySet().iterator().next().toString();
            setIfPresent(builder, "X-Tenant-Alias", tenantAlias);

            if (orgMap.get(tenantAlias) instanceof Map<?, ?> tenantData) {
                Object tenantId = tenantData.get("id");
                if (tenantId != null) {
                    setIfPresent(builder, "X-Tenant-Id", tenantId.toString());
                }
            }
        } else if (orgClaim != null) {
            log.warn("Unexpected organization claim format: {}", orgClaim.getClass().getSimpleName());
        }

        // Forward the original bearer token so downstream services can validate it themselves
        builder.header("Authorization", "Bearer " + jwt.getTokenValue());

        return builder.build();
    }

    private void setIfPresent(ServerHttpRequest.Builder builder, String header, String value) {
        if (value != null && !value.isBlank()) {
            builder.header(header, value);
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
