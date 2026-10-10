package com.karel.webhookinbox.common.web;

import com.karel.webhookinbox.config.WebhookInboxProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RateLimitFilter implements Filter {

    private static final int RETRY_AFTER_SECONDS = 60;

    private final WebhookInboxProperties properties;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!properties.rateLimit().enabled()) {
            chain.doFilter(request, response);
            return;
        }
        String token = tokenFrom(request);
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }
        Bucket bucket = buckets.computeIfAbsent(token, this::newBucket);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setStatus(429);
        httpResponse.setHeader("Retry-After", String.valueOf(RETRY_AFTER_SECONDS));
    }

    private Bucket newBucket(String token) {
        int requestsPerMinute = properties.rateLimit().requestsPerMinute();
        return Bucket.builder()
                .addLimit(Bandwidth.classic(requestsPerMinute, Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))))
                .build();
    }

    private String tokenFrom(ServletRequest request) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String path = (contextPath == null || contextPath.isEmpty()) ? uri : uri.substring(contextPath.length());
        if (!path.startsWith("/in/")) {
            return null;
        }
        String rest = path.substring(4);
        int slash = rest.indexOf('/');
        return slash == -1 ? rest : rest.substring(0, slash);
    }
}