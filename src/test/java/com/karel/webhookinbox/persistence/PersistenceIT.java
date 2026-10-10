package com.karel.webhookinbox.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.karel.webhookinbox.AbstractJpaTest;
import com.karel.webhookinbox.inbox.domain.Inbox;
import com.karel.webhookinbox.inbox.domain.InboxRepository;
import com.karel.webhookinbox.webhook.domain.WebhookRequest;
import com.karel.webhookinbox.webhook.domain.WebhookRequestRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

class PersistenceIT extends AbstractJpaTest {

    @Autowired
    private InboxRepository inboxRepository;

    @Autowired
    private WebhookRequestRepository webhookRequestRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void flywayAppliesV1AndCreatesTables() {
        Integer applied = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success = true",
                Integer.class);
        assertThat(applied).isEqualTo(1);

        Integer tables = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'public' "
                        + "and table_name in ('inbox', 'webhook_request')",
                Integer.class);
        assertThat(tables).isEqualTo(2);
    }

    @Test
    void savesAndFindsInboxByToken() {
        Inbox saved = saveInbox("token-abc");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        Inbox found = inboxRepository.findByToken("token-abc").orElseThrow();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getName()).isEqualTo("Inbox token-abc");
        assertThat(found.isActive()).isTrue();
    }

    @Test
    void returnsEmptyWhenTokenDoesNotExist() {
        assertThat(inboxRepository.findByToken("missing-token")).isEmpty();
    }

    @Test
    void preservesMultiValuedHeaders() {
        Inbox inbox = saveInbox("token-headers");

        WebhookRequest request = new WebhookRequest();
        request.setInboxId(inbox.getId());
        request.setMethod("POST");
        request.setPath("/hooks");
        request.setHeaders(Map.of("Accept", List.of("application/json", "text/plain")));
        request.setBodySize(0);
        Long id = webhookRequestRepository.saveAndFlush(request).getId();
        entityManager.clear();

        WebhookRequest reloaded = webhookRequestRepository.findById(id).orElseThrow();

        assertThat(reloaded.getHeaders())
                .containsEntry("Accept", List.of("application/json", "text/plain"));
        assertThat(reloaded.getReceivedAt()).isNotNull();
    }

    @Test
    void deletingInboxCascadesToItsRequests() {
        Inbox inbox = saveInbox("token-cascade");
        saveRequest(inbox, "GET");
        saveRequest(inbox, "POST");
        webhookRequestRepository.flush();

        assertThat(webhookRequestRepository.count()).isEqualTo(2);

        Long inboxId = inbox.getId();
        entityManager.clear();

        inboxRepository.deleteById(inboxId);
        inboxRepository.flush();

        assertThat(webhookRequestRepository.count()).isZero();
    }

    @Test
    void paginatesRequestsByInbox() {
        Inbox inbox = saveInbox("token-pages");
        for (int i = 0; i < 30; i++) {
            saveRequest(inbox, "POST");
        }
        webhookRequestRepository.flush();

        Page<WebhookRequest> secondPage =
                webhookRequestRepository.findByInboxIdOrderByReceivedAtDesc(inbox.getId(), PageRequest.of(1, 10));

        assertThat(secondPage.getTotalElements()).isEqualTo(30);
        assertThat(secondPage.getTotalPages()).isEqualTo(3);
        assertThat(secondPage.getNumberOfElements()).isEqualTo(10);
        assertThat(secondPage.getContent()).hasSize(10);
    }

    private Inbox saveInbox(String token) {
        Inbox inbox = new Inbox();
        inbox.setToken(token);
        inbox.setName("Inbox " + token);
        inbox.setActive(true);
        return inboxRepository.saveAndFlush(inbox);
    }

    private void saveRequest(Inbox inbox, String method) {
        WebhookRequest request = new WebhookRequest();
        request.setInboxId(inbox.getId());
        request.setMethod(method);
        request.setPath("/hooks");
        request.setHeaders(Map.of("Content-Type", List.of("application/json")));
        request.setBodySize(0);
        webhookRequestRepository.save(request);
    }
}
