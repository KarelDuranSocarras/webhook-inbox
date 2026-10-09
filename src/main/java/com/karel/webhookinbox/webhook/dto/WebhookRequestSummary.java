package com.karel.webhookinbox.webhook.dto;

import com.karel.webhookinbox.webhook.domain.WebhookRequest;
import java.time.Instant;

public record WebhookRequestSummary(
        Long id,
        String method,
        String path,
        String contentType,
        int bodySize,
        String sourceIp,
        Instant receivedAt
) {

    public static WebhookRequestSummary from(WebhookRequest request) {
        return new WebhookRequestSummary(
                request.getId(),
                request.getMethod(),
                request.getPath(),
                request.getContentType(),
                request.getBodySize(),
                request.getSourceIp(),
                request.getReceivedAt()
        );
    }
}
