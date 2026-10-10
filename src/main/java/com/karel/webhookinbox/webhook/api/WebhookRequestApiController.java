package com.karel.webhookinbox.webhook.api;

import com.karel.webhookinbox.common.web.PageResponse;
import com.karel.webhookinbox.inbox.application.InboxService;
import com.karel.webhookinbox.webhook.application.WebhookRequestService;
import com.karel.webhookinbox.webhook.dto.WebhookRequestDetail;
import com.karel.webhookinbox.webhook.dto.WebhookRequestSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inboxes/{token}/requests")
@Tag(name = "Webhook requests", description = "Inspect captured webhook requests")
@RequiredArgsConstructor
public class WebhookRequestApiController {

    private final InboxService inboxService;
    private final WebhookRequestService webhookRequestService;

    @GetMapping
    @Operation(summary = "List captured requests for an inbox")
    @ApiResponse(responseCode = "404", description = "Inbox not found")
    public PageResponse<WebhookRequestSummary> list(
            @PathVariable String token,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "0") int size) {
        Long inboxId = resolveInboxId(token);
        return PageResponse.from(webhookRequestService.list(inboxId, method, q, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a captured request")
    @ApiResponse(responseCode = "404", description = "Inbox or request not found")
    public WebhookRequestDetail get(@PathVariable String token, @PathVariable Long id) {
        return webhookRequestService.get(resolveInboxId(token), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a captured request")
    @ApiResponse(responseCode = "404", description = "Inbox or request not found")
    public void delete(@PathVariable String token, @PathVariable Long id) {
        webhookRequestService.delete(resolveInboxId(token), id);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete all captured requests for an inbox")
    @ApiResponse(responseCode = "404", description = "Inbox not found")
    public void clear(@PathVariable String token) {
        webhookRequestService.clear(resolveInboxId(token));
    }

    private Long resolveInboxId(String token) {
        return inboxService.getByToken(token).id();
    }
}
