package com.karel.webhookinbox.inbox.api;

import com.karel.webhookinbox.inbox.application.InboxService;
import com.karel.webhookinbox.inbox.dto.CreateInboxRequest;
import com.karel.webhookinbox.inbox.dto.InboxResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inboxes")
@Tag(name = "Inboxes", description = "Create and manage inboxes")
@RequiredArgsConstructor
public class InboxApiController {

    private final InboxService inboxService;

    @PostMapping
    @Operation(summary = "Create an inbox")
    @ApiResponse(responseCode = "400", description = "Invalid request payload")
    public ResponseEntity<InboxResponse> create(@Valid @RequestBody CreateInboxRequest request) {
        InboxResponse created = inboxService.create(request.name(), request.description());
        return ResponseEntity.created(URI.create("/api/inboxes/" + created.token())).body(created);
    }

    @GetMapping
    @Operation(summary = "List inboxes")
    public List<InboxResponse> list() {
        return inboxService.list();
    }

    @GetMapping("/{token}")
    @Operation(summary = "Get an inbox by token")
    @ApiResponse(responseCode = "404", description = "Inbox not found")
    public InboxResponse get(@PathVariable String token) {
        return inboxService.getByToken(token);
    }

    @DeleteMapping("/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an inbox")
    @ApiResponse(responseCode = "404", description = "Inbox not found")
    public void delete(@PathVariable String token) {
        inboxService.delete(token);
    }
}
