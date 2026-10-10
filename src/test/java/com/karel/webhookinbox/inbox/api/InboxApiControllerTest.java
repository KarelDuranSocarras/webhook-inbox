package com.karel.webhookinbox.inbox.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.karel.webhookinbox.common.error.ResourceNotFoundException;
import com.karel.webhookinbox.inbox.application.InboxService;
import com.karel.webhookinbox.inbox.dto.InboxResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InboxApiController.class)
class InboxApiControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InboxService inboxService;

    @Test
    void createsInboxWithRelativeLocationHeader() throws Exception {
        when(inboxService.create(eq("My inbox"), eq("desc")))
                .thenReturn(new InboxResponse(1L, "tok123", "My inbox", "desc", true, CREATED_AT));

        mockMvc.perform(post("/api/inboxes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"My inbox\",\"description\":\"desc\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/inboxes/tok123"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.token").value("tok123"));
    }

    @Test
    void rejectsInvalidRequestWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/inboxes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verify(inboxService, never()).create(any(), any());
    }

    @Test
    void listsInboxes() throws Exception {
        when(inboxService.list())
                .thenReturn(List.of(new InboxResponse(1L, "abc", "A", null, true, CREATED_AT)));

        mockMvc.perform(get("/api/inboxes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].token").value("abc"));
    }

    @Test
    void returnsNotFoundForUnknownToken() throws Exception {
        when(inboxService.getByToken("missing"))
                .thenThrow(new ResourceNotFoundException("Inbox not found: missing"));

        mockMvc.perform(get("/api/inboxes/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Inbox not found: missing"));
    }

    @Test
    void deletesInbox() throws Exception {
        mockMvc.perform(delete("/api/inboxes/tok123"))
                .andExpect(status().isNoContent());

        verify(inboxService).delete("tok123");
    }
}
