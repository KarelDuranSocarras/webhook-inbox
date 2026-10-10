package com.karel.webhookinbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "webhook-inbox")
public record WebhookInboxProperties(
        long maxBodyBytes,
        int tokenLength,
        int defaultPageSize,
        int retentionDays,
        RateLimit rateLimit,
        String baseUrl,
        boolean retentionEnabled,
        String retentionCron
) {

    public record RateLimit(boolean enabled, int requestsPerMinute) {
    }
}
