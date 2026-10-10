package com.karel.webhookinbox.config;

import com.karel.webhookinbox.webhook.application.RetentionJob;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "webhook-inbox.retention-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class SchedulingConfig {

    private final RetentionJob retentionJob;

    @Scheduled(cron = "${webhook-inbox.retention-cron}")
    public void runRetention() {
        retentionJob.run();
    }
}