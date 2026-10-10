package com.karel.webhookinbox.webhook.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.karel.webhookinbox.common.error.PayloadTooLargeException;
import com.karel.webhookinbox.common.error.ResourceNotFoundException;
import com.karel.webhookinbox.inbox.application.InboxService;
import com.karel.webhookinbox.inbox.dto.InboxResponse;
import com.karel.webhookinbox.webhook.application.WebhookCaptureService;
import com.karel.webhookinbox.webhook.dto.IncomingWebhook;
import com.karel.webhookinbox.webhook.dto.WebhookRequestDetail;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IngestController.class)
class IngestControllerTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InboxService inboxService;

    @MockitoBean
    private WebhookCaptureService webhookCaptureService;

    @BeforeEach
    void setUp() {
        when(inboxService.getByToken("tok"))
                .thenReturn(new InboxResponse(1L, "tok", "Inbox", null, true, RECEIVED_AT));
        when(webhookCaptureService.capture(eq(1L), eq(true), any(IncomingWebhook.class)))
                .thenReturn(detail());
    }

    @Test
    void capturesPostJsonBody() throws Exception {
        mockMvc.perform(post("/in/tok/hooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hello\":\"world\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));

        IncomingWebhook incoming = captured();
        assertThat(incoming.method()).isEqualTo("POST");
        assertThat(incoming.path()).isEqualTo("/hooks");
        assertThat(incoming.contentType()).isEqualTo("application/json");
    }

    @Test
    void capturesGetOnTokenRoot() throws Exception {
        mockMvc.perform(get("/in/tok"))
                .andExpect(status().isOk());

        IncomingWebhook incoming = captured();
        assertThat(incoming.method()).isEqualTo("GET");
        assertThat(incoming.path()).isEqualTo("/");
    }

    @Test
    void resolvesPathWithoutContextPath() throws Exception {
        mockMvc.perform(get("/in/tok/stripe/event"))
                .andExpect(status().isOk());

        assertThat(captured().path()).isEqualTo("/stripe/event");
    }

    @Test
    void resolvesPathWithContextPath() throws Exception {
        mockMvc.perform(get("/myapp/in/tok/stripe/event").contextPath("/myapp"))
                .andExpect(status().isOk());

        assertThat(captured().path()).isEqualTo("/stripe/event");
    }

    @Test
    void resolvesRootPathWhenSuffixEmptyWithContextPath() throws Exception {
        mockMvc.perform(get("/myapp/in/tok").contextPath("/myapp"))
                .andExpect(status().isOk());

        assertThat(captured().path()).isEqualTo("/");
    }

    @Test
    void preservesMultiValuedHeaders() throws Exception {
        mockMvc.perform(get("/in/tok").header("X-Test", "a", "b"))
                .andExpect(status().isOk());

        assertThat(captured().headers()).containsEntry("X-Test", List.of("a", "b"));
    }

    @Test
    void capturesNullContentType() throws Exception {
        mockMvc.perform(get("/in/tok"))
                .andExpect(status().isOk());

        assertThat(captured().contentType()).isNull();
    }

    @Test
    void returnsNotFoundForUnknownToken() throws Exception {
        when(inboxService.getByToken("missing"))
                .thenThrow(new ResourceNotFoundException("Inbox not found: missing"));

        mockMvc.perform(get("/in/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsPayloadTooLargeWhenBodyExceedsLimit() throws Exception {
        when(webhookCaptureService.capture(eq(1L), eq(true), any(IncomingWebhook.class)))
                .thenThrow(new PayloadTooLargeException("Request body exceeds the maximum of 8 bytes"));

        mockMvc.perform(post("/in/tok/hooks")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("0123456789"))
                .andExpect(status().isPayloadTooLarge());
    }

    private IncomingWebhook captured() {
        ArgumentCaptor<IncomingWebhook> captor = ArgumentCaptor.forClass(IncomingWebhook.class);
        verify(webhookCaptureService).capture(eq(1L), eq(true), captor.capture());
        return captor.getValue();
    }

    private WebhookRequestDetail detail() {
        return new WebhookRequestDetail(10L, "POST", "/hooks", null, Map.of(),
                "application/json", "{\"hello\":\"world\"}", 17, "127.0.0.1", RECEIVED_AT);
    }
}
