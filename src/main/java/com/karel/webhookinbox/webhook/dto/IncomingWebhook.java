package com.karel.webhookinbox.webhook.dto;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public record IncomingWebhook(
        String method,
        String path,
        String queryString,
        Map<String, List<String>> headers,
        String contentType,
        String remoteAddr,
        InputStream body
) {
}
