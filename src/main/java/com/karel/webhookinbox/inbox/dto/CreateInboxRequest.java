package com.karel.webhookinbox.inbox.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInboxRequest(
        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,
        @Size(max = 500, message = "description must be at most 500 characters")
        String description
) {
}
