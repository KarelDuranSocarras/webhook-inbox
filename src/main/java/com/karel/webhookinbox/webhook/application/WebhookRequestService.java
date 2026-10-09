package com.karel.webhookinbox.webhook.application;

import com.karel.webhookinbox.common.error.ResourceNotFoundException;
import com.karel.webhookinbox.config.WebhookInboxProperties;
import com.karel.webhookinbox.webhook.domain.WebhookRequest;
import com.karel.webhookinbox.webhook.domain.WebhookRequestRepository;
import com.karel.webhookinbox.webhook.dto.WebhookRequestDetail;
import com.karel.webhookinbox.webhook.dto.WebhookRequestSummary;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebhookRequestService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort RECEIVED_AT_DESC = Sort.by(Sort.Direction.DESC, "receivedAt");

    private final WebhookRequestRepository webhookRequestRepository;
    private final WebhookInboxProperties properties;

    @Transactional(readOnly = true)
    public Page<WebhookRequestSummary> list(Long inboxId, String method, String q, Pageable pageable) {
        String normalizedMethod = (method == null || method.isBlank()) ? null : method.trim().toUpperCase(Locale.ROOT);
        String normalizedQuery = (q == null || q.isBlank()) ? null : q.trim();
        return webhookRequestRepository.search(inboxId, normalizedMethod, normalizedQuery, normalize(pageable))
                .map(WebhookRequestSummary::from);
    }

    @Transactional(readOnly = true)
    public WebhookRequestDetail get(Long inboxId, Long requestId) {
        return WebhookRequestDetail.from(require(inboxId, requestId));
    }

    @Transactional
    public void delete(Long inboxId, Long requestId) {
        webhookRequestRepository.delete(require(inboxId, requestId));
    }

    @Transactional
    public int clear(Long inboxId) {
        return webhookRequestRepository.deleteByInboxId(inboxId);
    }

    private WebhookRequest require(Long inboxId, Long requestId) {
        return webhookRequestRepository.findByIdAndInboxId(requestId, inboxId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook request not found: " + requestId));
    }

    private Pageable normalize(Pageable pageable) {
        int size = pageable.getPageSize() <= 0
                ? properties.defaultPageSize()
                : Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        return PageRequest.of(pageable.getPageNumber(), size, RECEIVED_AT_DESC);
    }
}
