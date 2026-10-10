package com.karel.webhookinbox.inbox.dto;

import com.karel.webhookinbox.inbox.domain.Inbox;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record InboxResponse(
        Long id,
        @Schema(description = "Token used to build the ingest URL (/in/{token})")
        String token,
        String name,
        String description,
        boolean active,
        Instant createdAt
) {

    public static InboxResponse from(Inbox inbox) {
        return new InboxResponse(
                inbox.getId(),
                inbox.getToken(),
                inbox.getName(),
                inbox.getDescription(),
                inbox.isActive(),
                inbox.getCreatedAt()
        );
    }
}
