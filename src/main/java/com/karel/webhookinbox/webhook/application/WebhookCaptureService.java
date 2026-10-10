package com.karel.webhookinbox.webhook.application;

import com.karel.webhookinbox.common.error.PayloadTooLargeException;
import com.karel.webhookinbox.common.error.ResourceNotFoundException;
import com.karel.webhookinbox.common.web.ClientIpResolver;
import com.karel.webhookinbox.config.WebhookInboxProperties;
import com.karel.webhookinbox.webhook.domain.WebhookRequest;
import com.karel.webhookinbox.webhook.domain.WebhookRequestRepository;
import com.karel.webhookinbox.webhook.dto.IncomingWebhook;
import com.karel.webhookinbox.webhook.dto.WebhookRequestDetail;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookCaptureService {

    private static final int READ_CHUNK = 8192;

    private final WebhookRequestRepository webhookRequestRepository;
    private final ClientIpResolver clientIpResolver;
    private final WebhookInboxProperties properties;

    public WebhookRequestDetail capture(Long inboxId, boolean inboxActive, IncomingWebhook incoming) {
        if (!inboxActive) {
            throw new ResourceNotFoundException("Inbox not found");
        }
        long maxBodyBytes = properties.maxBodyBytes();
        byte[] payload = readLimited(incoming.body(), maxBodyBytes);

        WebhookRequest request = new WebhookRequest();
        request.setInboxId(inboxId);
        request.setMethod(incoming.method());
        request.setPath(incoming.path());
        request.setQueryString(incoming.queryString());
        request.setHeaders(incoming.headers() == null ? Map.of() : incoming.headers());
        request.setContentType(incoming.contentType());
        request.setBodySize(payload.length);
        request.setSourceIp(clientIpResolver.resolve(incoming.headers(), incoming.remoteAddr()));
        request.setBody(isTextual(incoming.contentType())
                ? new String(payload, StandardCharsets.UTF_8)
                : "[binary body, " + payload.length + " bytes, not stored]");

        WebhookRequest saved = webhookRequestRepository.save(request);
        log.debug("Captured {} {} for inbox {} ({} bytes)",
                saved.getMethod(), saved.getPath(), inboxId, saved.getBodySize());
        return WebhookRequestDetail.from(saved);
    }

    private byte[] readLimited(InputStream body, long maxBytes) {
        if (body == null) {
            return new byte[0];
        }
        long limit = maxBytes + 1;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[READ_CHUNK];
        long total = 0;
        try (InputStream in = body) {
            int read;
            while (total < limit
                    && (read = in.read(chunk, 0, (int) Math.min(chunk.length, limit - total))) != -1) {
                buffer.write(chunk, 0, read);
                total += read;
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read webhook request body", ex);
        }
        if (total > maxBytes) {
            throw new PayloadTooLargeException(
                    "Request body exceeds the maximum of " + maxBytes + " bytes");
        }
        return buffer.toByteArray();
    }

    private boolean isTextual(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return normalized.startsWith("text/")
                || normalized.contains("json")
                || normalized.contains("xml")
                || normalized.contains("form-urlencoded");
    }
}
