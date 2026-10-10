package com.karel.webhookinbox.inbox.view;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.karel.webhookinbox.common.error.ValidationException;
import com.karel.webhookinbox.common.web.IngestUrlResolver;
import com.karel.webhookinbox.inbox.application.InboxService;
import com.karel.webhookinbox.inbox.dto.InboxResponse;
import com.karel.webhookinbox.webhook.application.WebhookRequestService;
import java.time.Instant;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InboxViewController.class)
@Import(ThymeleafAutoConfiguration.class)
class InboxViewControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final InboxResponse INBOX = new InboxResponse(1L, "abc", "My inbox", "desc", true, CREATED_AT);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InboxService inboxService;

    @MockitoBean
    private WebhookRequestService webhookRequestService;

    @MockitoBean
    private IngestUrlResolver ingestUrlResolver;

    @Test
    void homeRedirectsToInboxes() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inboxes"));
    }

    @Test
    void listsInboxesRenderingTemplate() throws Exception {
        when(inboxService.list()).thenReturn(List.of(INBOX));

        mockMvc.perform(get("/inboxes"))
                .andExpect(status().isOk())
                .andExpect(view().name("inboxes"))
                .andExpect(model().attribute("inboxes", List.of(INBOX)))
                .andExpect(content().string(Matchers.containsString("My inbox")))
                .andExpect(content().string(Matchers.containsString("/inboxes/abc")));
    }

    @Test
    void rendersEmptyStateWhenThereAreNoInboxes() throws Exception {
        when(inboxService.list()).thenReturn(List.of());

        mockMvc.perform(get("/inboxes"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("No inboxes yet")));
    }

    @Test
    void createRedirectsToNewInboxWithFlashMessage() throws Exception {
        when(inboxService.create(eq("My inbox"), eq("desc"))).thenReturn(INBOX);

        mockMvc.perform(post("/inboxes").param("name", "My inbox").param("description", "desc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inboxes/abc"))
                .andExpect(flash().attribute("message", "Inbox created"));
    }

    @Test
    void createReRendersFormWhenValidationFails() throws Exception {
        when(inboxService.create(eq("   "), eq(null)))
                .thenThrow(new ValidationException("Name must not be blank"));
        when(inboxService.list()).thenReturn(List.of());

        mockMvc.perform(post("/inboxes").param("name", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("inboxes"))
                .andExpect(model().attribute("error", "Name must not be blank"))
                .andExpect(content().string(Matchers.containsString("Name must not be blank")));
    }

    @Test
    void detailRendersInboxWithIngestUrlAndRequests() throws Exception {
        Page<com.karel.webhookinbox.webhook.dto.WebhookRequestSummary> empty = new PageImpl<>(List.of());
        when(inboxService.getByToken("abc")).thenReturn(INBOX);
        when(ingestUrlResolver.resolve("abc")).thenReturn("https://hooks.example.com/in/abc");
        when(webhookRequestService.list(eq(1L), eq(null), eq(null), eq(0), eq(0))).thenReturn(empty);

        mockMvc.perform(get("/inboxes/abc"))
                .andExpect(status().isOk())
                .andExpect(view().name("inbox"))
                .andExpect(model().attribute("ingestUrl", "https://hooks.example.com/in/abc"))
                .andExpect(content().string(Matchers.containsString("https://hooks.example.com/in/abc")))
                .andExpect(content().string(Matchers.containsString("No requests yet")));
    }

    @Test
    void deleteRedirectsToInboxesWithFlashMessage() throws Exception {
        mockMvc.perform(post("/inboxes/abc/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inboxes"))
                .andExpect(flash().attribute("message", "Inbox deleted"));
    }
}
