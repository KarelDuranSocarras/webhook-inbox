package com.karel.webhookinbox.webhook.view;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.karel.webhookinbox.common.error.ResourceNotFoundException;
import com.karel.webhookinbox.common.web.IngestUrlResolver;
import com.karel.webhookinbox.inbox.application.InboxService;
import com.karel.webhookinbox.inbox.dto.InboxResponse;
import com.karel.webhookinbox.inbox.view.InboxViewController;
import com.karel.webhookinbox.webhook.application.WebhookRequestService;
import com.karel.webhookinbox.webhook.dto.WebhookRequestDetail;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {InboxViewController.class, WebhookRequestViewController.class})
@Import(ThymeleafAutoConfiguration.class)
class WebhookRequestViewControllerTest {

    private static final String LIST_FRAGMENT = "fragments/request-list :: list";
    private static final Instant RECEIVED_AT = Instant.parse("2026-01-01T12:00:00Z");
    private static final InboxResponse INBOX = new InboxResponse(1L, "abc", "My inbox", null, true, RECEIVED_AT);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InboxService inboxService;

    @MockitoBean
    private WebhookRequestService webhookRequestService;

    @MockitoBean
    private IngestUrlResolver ingestUrlResolver;

    @BeforeEach
    void setUp() {
        when(inboxService.getByToken("abc")).thenReturn(INBOX);
        when(ingestUrlResolver.resolve("abc")).thenReturn("http://localhost/in/abc");
        when(webhookRequestService.list(eq(1L), any(), any(), eq(0), eq(0))).thenReturn(emptyPage());
    }

    @Test
    void resolvesViewRoutesWithoutAmbiguity() throws Exception {
        when(webhookRequestService.get(1L, 123L)).thenReturn(detail("<script>alert(1)</script>"));
        when(webhookRequestService.get(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Webhook request not found: 999"));

        mockMvc.perform(get("/inboxes/abc"))
                .andExpect(status().isOk())
                .andExpect(view().name("inbox"));

        mockMvc.perform(get("/inboxes/abc/requests"))
                .andExpect(status().isOk())
                .andExpect(view().name(LIST_FRAGMENT));

        mockMvc.perform(get("/inboxes/abc/requests/123"))
                .andExpect(status().isOk())
                .andExpect(view().name("request"));

        mockMvc.perform(get("/inboxes/abc/requests/999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"));
    }

    @Test
    void listForwardsFiltersAndReturnsFragment() throws Exception {
        mockMvc.perform(get("/inboxes/abc/requests").param("method", "POST").param("q", "/hooks"))
                .andExpect(status().isOk())
                .andExpect(view().name(LIST_FRAGMENT));

        verify(webhookRequestService).list(1L, "POST", "/hooks", 0, 0);
    }

    @Test
    void detailRendersRequestWithCurlAndUrl() throws Exception {
        when(webhookRequestService.get(1L, 123L)).thenReturn(detail("{\"hello\":\"world\"}"));

        mockMvc.perform(get("/inboxes/abc/requests/123"))
                .andExpect(status().isOk())
                .andExpect(view().name("request"))
                .andExpect(model().attribute("request", detail("{\"hello\":\"world\"}")))
                .andExpect(content().string(Matchers.containsString("http://localhost/in/abc/hooks?a=1")))
                .andExpect(content().string(Matchers.containsString("curl -X POST")));
    }

    @Test
    void escapesMaliciousBodyWhenRendering() throws Exception {
        when(webhookRequestService.get(1L, 123L)).thenReturn(detail("<script>alert(1)</script>"));

        mockMvc.perform(get("/inboxes/abc/requests/123"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("<script>alert(1)</script>"))));
    }

    @Test
    void deleteRendersUpdatedFragment() throws Exception {
        mockMvc.perform(post("/inboxes/abc/requests/123/delete"))
                .andExpect(status().isOk())
                .andExpect(view().name(LIST_FRAGMENT));

        verify(webhookRequestService).delete(1L, 123L);
    }

    @Test
    void clearRendersUpdatedFragment() throws Exception {
        mockMvc.perform(post("/inboxes/abc/requests/clear"))
                .andExpect(status().isOk())
                .andExpect(view().name(LIST_FRAGMENT));

        verify(webhookRequestService).clear(1L);
    }

    private static Page<com.karel.webhookinbox.webhook.dto.WebhookRequestSummary> emptyPage() {
        return new PageImpl<>(List.of());
    }

    private static WebhookRequestDetail detail(String body) {
        return new WebhookRequestDetail(
                123L,
                "POST",
                "/hooks",
                "a=1",
                Map.of("Content-Type", List.of("application/json")),
                "application/json",
                body,
                body.length(),
                "127.0.0.1",
                RECEIVED_AT
        );
    }
}
