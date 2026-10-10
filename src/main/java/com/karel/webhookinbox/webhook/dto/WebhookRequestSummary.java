package com.karel.webhookinbox.webhook.dto;

import com.karel.webhookinbox.webhook.domain.WebhookRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record WebhookRequestSummary(
        Long id,
        String method,
        String path,
        String contentType,
        @Schema(description = "Raw body size in bytes")
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
