package com.karel.webhookinbox.webhook.application;

import com.karel.webhookinbox.config.WebhookInboxProperties;
import com.karel.webhookinbox.webhook.domain.WebhookRequestRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetentionJob {

    private final WebhookRequestRepository webhookRequestRepository;
    private final WebhookInboxProperties properties;

    @Transactional
    public int run() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(properties.retentionDays()));
        int deleted = webhookRequestRepository.deleteOlderThan(cutoff);
        log.info("Retention: deleted {} webhook request(s) older than {}", deleted, cutoff);
        return deleted;
    }
}