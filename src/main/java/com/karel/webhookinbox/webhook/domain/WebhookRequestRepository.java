package com.karel.webhookinbox.webhook.domain;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface WebhookRequestRepository extends JpaRepository<WebhookRequest, Long> {

    Page<WebhookRequest> findByInboxIdOrderByReceivedAtDesc(Long inboxId, Pageable pageable);

    Optional<WebhookRequest> findByIdAndInboxId(Long id, Long inboxId);

    @Query("""
            select w from WebhookRequest w
            where w.inboxId = :inboxId
              and (:method is null or w.method = :method)
              and (:q is null or w.path ilike concat('%', cast(:q as string), '%'))
            """)
    Page<WebhookRequest> search(@Param("inboxId") Long inboxId,
                                @Param("method") String method,
                                @Param("q") String q,
                                Pageable pageable);

    @Modifying
    @Transactional
    @Query("delete from WebhookRequest w where w.inboxId = :inboxId")
    int deleteByInboxId(@Param("inboxId") Long inboxId);

    @Modifying
    @Transactional
    @Query("delete from WebhookRequest w where w.receivedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
