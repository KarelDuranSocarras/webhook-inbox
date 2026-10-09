package com.karel.webhookinbox.webhook.dto;

import com.karel.webhookinbox.webhook.domain.WebhookRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record WebhookRequestDetail(
        Long id,
        String method,
        String path,
        String queryString,
        Map<String, List<String>> headers,
        String contentType,
        String body,
        int bodySize,
        String sourceIp,
        Instant receivedAt
) {

    public static WebhookRequestDetail from(WebhookRequest request) {
        return new WebhookRequestDetail(
                request.getId(),
                request.getMethod(),
                request.getPath(),
                request.getQueryString(),
                request.getHeaders(),
                request.getContentType(),
                request.getBody(),
                request.getBodySize(),
                request.getSourceIp(),
                request.getReceivedAt()
        );
    }
}
