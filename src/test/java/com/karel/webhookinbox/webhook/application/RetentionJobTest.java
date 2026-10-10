package com.karel.webhookinbox.webhook.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.karel.webhookinbox.config.WebhookInboxProperties;
import com.karel.webhookinbox.config.WebhookInboxProperties.RateLimit;
import com.karel.webhookinbox.webhook.domain.WebhookRequestRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetentionJobTest {

    @Mock
    private WebhookRequestRepository webhookRequestRepository;

    private RetentionJob job;

    @BeforeEach
    void setUp() {
        WebhookInboxProperties properties =
                new WebhookInboxProperties(1_048_576L, 24, 25, 7, new RateLimit(true, 100), null, true, "0 0 * * * *");
        job = new RetentionJob(webhookRequestRepository, properties);
    }

    @Test
    void deletesRequestsOlderThanCutoff() {
        when(webhookRequestRepository.deleteOlderThan(any())).thenReturn(4);

        assertThat(job.run()).isEqualTo(4);
        verify(webhookRequestRepository).deleteOlderThan(any());
    }

    @Test
    void usesCutoffSevenDaysAgoSoRecentRequestsSurvive() {
        when(webhookRequestRepository.deleteOlderThan(any())).thenReturn(0);

        job.run();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(webhookRequestRepository).deleteOlderThan(captor.capture());
        Instant cutoff = captor.getValue();
        Instant now = Instant.now();
        assertThat(cutoff).isBefore(now);
        assertThat(cutoff).isBetween(
                now.minus(Duration.ofDays(7)).minusSeconds(5),
                now.minus(Duration.ofDays(7)).plusSeconds(5));
    }
}