package com.karel.webhookinbox.webhook.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookRequestRepository extends JpaRepository<WebhookRequest, Long> {

    Page<WebhookRequest> findByInboxIdOrderByReceivedAtDesc(Long inboxId, Pageable pageable);
}
