package com.karel.webhookinbox.webhook.api;

import com.karel.webhookinbox.inbox.application.InboxService;
import com.karel.webhookinbox.inbox.dto.InboxResponse;
import com.karel.webhookinbox.webhook.application.WebhookCaptureService;
import com.karel.webhookinbox.webhook.dto.IncomingWebhook;
import com.karel.webhookinbox.webhook.dto.IngestResponse;
import com.karel.webhookinbox.webhook.dto.WebhookRequestDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Ingestion", description = "Webhook ingestion endpoint")
@RequiredArgsConstructor
public class IngestController {

    private final InboxService inboxService;
    private final WebhookCaptureService webhookCaptureService;

    @RequestMapping({"/in/{token}", "/in/{token}/**"})
    @Operation(summary = "Receive a webhook and capture it")
    @ApiResponse(responseCode = "404", description = "Inbox not found")
    @ApiResponse(responseCode = "413", description = "Payload too large")
    public IngestResponse ingest(@PathVariable String token, HttpServletRequest request) {
        InboxResponse inbox = inboxService.getByToken(token);
        IncomingWebhook incoming = new IncomingWebhook(
                request.getMethod(),
                resolvePath(request, token),
                request.getQueryString(),
                headers(request),
                request.getContentType(),
                request.getRemoteAddr(),
                inputStream(request));
        WebhookRequestDetail detail = webhookCaptureService.capture(inbox.id(), inbox.active(), incoming);
        return new IngestResponse(detail.id(), detail.receivedAt());
    }

    private String resolvePath(HttpServletRequest request, String token) {
        String uri = request.getRequestURI();
        String prefix = request.getContextPath() + "/in/" + token;
        String suffix = uri.length() > prefix.length() ? uri.substring(prefix.length()) : "";
        return suffix.isEmpty() ? "/" : suffix;
    }

    private Map<String, List<String>> headers(HttpServletRequest request) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                headers.put(name, Collections.list(request.getHeaders(name)));
            }
        }
        return headers;
    }

    private InputStream inputStream(HttpServletRequest request) {
        try {
            return request.getInputStream();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read request body", ex);
        }
    }
}
