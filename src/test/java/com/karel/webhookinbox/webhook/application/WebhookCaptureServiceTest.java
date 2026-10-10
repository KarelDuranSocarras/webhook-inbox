package com.karel.webhookinbox.webhook.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.karel.webhookinbox.common.error.PayloadTooLargeException;
import com.karel.webhookinbox.common.error.ResourceNotFoundException;
import com.karel.webhookinbox.common.web.ClientIpResolver;
import com.karel.webhookinbox.config.WebhookInboxProperties;
import com.karel.webhookinbox.config.WebhookInboxProperties.RateLimit;
import com.karel.webhookinbox.webhook.domain.WebhookRequest;
import com.karel.webhookinbox.webhook.domain.WebhookRequestRepository;
import com.karel.webhookinbox.webhook.dto.IncomingWebhook;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookCaptureServiceTest {

    private static final long MAX_BODY_BYTES = 8L;
    private static final long INBOX_ID = 1L;

    @Mock
    private WebhookRequestRepository webhookRequestRepository;

    private WebhookCaptureService service;

    @BeforeEach
    void setUp() {
        WebhookInboxProperties properties =
                new WebhookInboxProperties(MAX_BODY_BYTES, 24, 25, 7, new RateLimit(true, 100), null, true, "0 0 * * * *");
        service = new WebhookCaptureService(webhookRequestRepository, new ClientIpResolver(), properties);

        lenient().when(webhookRequestRepository.save(any(WebhookRequest.class))).thenAnswer(invocation -> {
            WebhookRequest request = invocation.getArgument(0);
            request.setId(10L);
            request.setReceivedAt(Instant.now());
            return request;
        });
    }

    @Test
    void capturesTextBody() {
        IncomingWebhook incoming = incoming("POST", "/stripe/event", "application/json",
                "{\"a\":1}", Map.of("Content-Type", List.of("application/json")), "192.168.1.10");

        service.capture(INBOX_ID, true, incoming);

        WebhookRequest saved = captured();
        assertThat(saved.getMethod()).isEqualTo("POST");
        assertThat(saved.getPath()).isEqualTo("/stripe/event");
        assertThat(saved.getBody()).isEqualTo("{\"a\":1}");
        assertThat(saved.getBodySize()).isEqualTo(7);
        assertThat(saved.getContentType()).isEqualTo("application/json");
        assertThat(saved.getInboxId()).isEqualTo(INBOX_ID);
    }

    @Test
    void rejectsOversizedBodyWithoutPersisting() {
        IncomingWebhook incoming = incoming("POST", "/large", "text/plain",
                "0123456789", Map.of(), "192.168.1.10");

        assertThatThrownBy(() -> service.capture(INBOX_ID, true, incoming))
                .isInstanceOf(PayloadTooLargeException.class);
        verify(webhookRequestRepository, never()).save(any(WebhookRequest.class));
    }

    @Test
    void rejectsInactiveInboxWithoutPersisting() {
        IncomingWebhook incoming = incoming("POST", "/inactive", "text/plain", "hi", Map.of(), "192.168.1.10");

        assertThatThrownBy(() -> service.capture(INBOX_ID, false, incoming))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Inbox not found");
        verify(webhookRequestRepository, never()).save(any(WebhookRequest.class));
    }

    @Test
    void storesMarkerForBinaryBody() {
        byte[] binary = {0, 1, 2, 3, 4};
        IncomingWebhook incoming = new IncomingWebhook("POST", "/binary", null, Map.of(),
                "application/octet-stream", "192.168.1.10", new ByteArrayInputStream(binary));

        service.capture(INBOX_ID, true, incoming);

        WebhookRequest saved = captured();
        assertThat(saved.getBody()).isEqualTo("[binary body, 5 bytes, not stored]");
        assertThat(saved.getBodySize()).isEqualTo(5);
    }

    @Test
    void prefersForwardedForOverRemoteAddress() {
        IncomingWebhook incoming = incoming("POST", "/x", "text/plain", "hi",
                Map.of("X-Forwarded-For", List.of("203.0.113.7, 10.0.0.1")), "192.168.1.10");

        service.capture(INBOX_ID, true, incoming);

        assertThat(captured().getSourceIp()).isEqualTo("203.0.113.7");
    }

    @Test
    void fallsBackToRemoteAddress() {
        IncomingWebhook incoming = incoming("POST", "/x", "text/plain", "hi", Map.of(), "192.168.1.10");

        service.capture(INBOX_ID, true, incoming);

        assertThat(captured().getSourceIp()).isEqualTo("192.168.1.10");
    }

    private WebhookRequest captured() {
        ArgumentCaptor<WebhookRequest> captor = ArgumentCaptor.forClass(WebhookRequest.class);
        verify(webhookRequestRepository).save(captor.capture());
        return captor.getValue();
    }

    private IncomingWebhook incoming(String method, String path, String contentType, String body,
                                     Map<String, List<String>> headers, String remoteAddr) {
        return new IncomingWebhook(method, path, null, headers, contentType, remoteAddr,
                new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    }
}
