package com.karel.webhookinbox.webhook.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.karel.webhookinbox.common.error.ResourceNotFoundException;
import com.karel.webhookinbox.inbox.application.InboxService;
import com.karel.webhookinbox.inbox.dto.InboxResponse;
import com.karel.webhookinbox.webhook.application.WebhookRequestService;
import com.karel.webhookinbox.webhook.dto.WebhookRequestSummary;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WebhookRequestApiController.class)
class WebhookRequestApiControllerTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InboxService inboxService;

    @MockitoBean
    private WebhookRequestService webhookRequestService;

    @BeforeEach
    void setUp() {
        when(inboxService.getByToken("tok123"))
                .thenReturn(new InboxResponse(1L, "tok123", "Inbox", null, true, RECEIVED_AT));
    }

    @Test
    void listsRequestsWithPaginationMetadata() throws Exception {
        WebhookRequestSummary summary =
                new WebhookRequestSummary(9L, "POST", "/hooks", "application/json", 12, "127.0.0.1", RECEIVED_AT);
        when(webhookRequestService.list(eq(1L), isNull(), isNull(), eq(0), eq(25)))
                .thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 25), 1));

        mockMvc.perform(get("/api/inboxes/tok123/requests").param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(9))
                .andExpect(jsonPath("$.content[0].method").value("POST"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(25))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void returnsNotFoundWhenInboxMissing() throws Exception {
        when(inboxService.getByToken("missing"))
                .thenThrow(new ResourceNotFoundException("Inbox not found: missing"));

        mockMvc.perform(get("/api/inboxes/missing/requests"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletesSingleRequest() throws Exception {
        mockMvc.perform(delete("/api/inboxes/tok123/requests/9"))
                .andExpect(status().isNoContent());

        verify(webhookRequestService).delete(1L, 9L);
    }

    @Test
    void clearsInboxRequests() throws Exception {
        mockMvc.perform(delete("/api/inboxes/tok123/requests"))
                .andExpect(status().isNoContent());

        verify(webhookRequestService).clear(1L);
    }
}
