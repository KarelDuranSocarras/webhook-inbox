package com.karel.webhookinbox.webhook.dto;

import java.time.Instant;

public record IngestResponse(Long id, Instant receivedAt) {
}
