package com.karel.webhookinbox.common.web;

import com.karel.webhookinbox.config.WebhookInboxProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class IngestUrlResolver {

    private final WebhookInboxProperties properties;

    public String resolve(String token) {
        String base = properties.baseUrl();
        if (base == null || base.isBlank()) {
            base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/in/" + token;
    }
}
