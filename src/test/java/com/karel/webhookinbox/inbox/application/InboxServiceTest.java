package com.karel.webhookinbox.inbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.karel.webhookinbox.common.error.ResourceNotFoundException;
import com.karel.webhookinbox.common.error.ValidationException;
import com.karel.webhookinbox.common.util.TokenGenerator;
import com.karel.webhookinbox.config.WebhookInboxProperties;
import com.karel.webhookinbox.config.WebhookInboxProperties.RateLimit;
import com.karel.webhookinbox.inbox.domain.Inbox;
import com.karel.webhookinbox.inbox.domain.InboxRepository;
import com.karel.webhookinbox.inbox.dto.InboxResponse;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class InboxServiceTest {

    @Mock
    private InboxRepository inboxRepository;

    private InboxService service;

    @BeforeEach
    void setUp() {
        WebhookInboxProperties properties =
                new WebhookInboxProperties(1_048_576L, 24, 25, 7, new RateLimit(true, 100), null, true, "0 0 * * * *");
        service = new InboxService(inboxRepository, new TokenGenerator(), properties);
    }

    @Test
    void createsInboxWithTrimmedNameAndGeneratedToken() {
        when(inboxRepository.saveAndFlush(any(Inbox.class))).thenAnswer(invocation -> {
            Inbox inbox = invocation.getArgument(0);
            inbox.setId(1L);
            inbox.setCreatedAt(Instant.now());
            return inbox;
        });

        InboxResponse response = service.create("  My inbox  ", "  a description  ");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("My inbox");
        assertThat(response.description()).isEqualTo("a description");
        assertThat(response.token()).hasSize(24);
        assertThat(response.active()).isTrue();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> service.create("   ", null))
                .isInstanceOf(ValidationException.class);
        verify(inboxRepository, never()).saveAndFlush(any(Inbox.class));
    }

    @Test
    void rejectsTooLongName() {
        assertThatThrownBy(() -> service.create("x".repeat(121), null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsTooLongDescription() {
        assertThatThrownBy(() -> service.create("name", "x".repeat(501)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void retriesOnTokenCollision() {
        when(inboxRepository.saveAndFlush(any(Inbox.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate token"))
                .thenAnswer(invocation -> {
                    Inbox inbox = invocation.getArgument(0);
                    inbox.setId(2L);
                    inbox.setCreatedAt(Instant.now());
                    return inbox;
                });

        InboxResponse response = service.create("name", null);

        assertThat(response.id()).isEqualTo(2L);
        verify(inboxRepository, times(2)).saveAndFlush(any(Inbox.class));
    }

    @Test
    void failsAfterExhaustingTokenRetries() {
        when(inboxRepository.saveAndFlush(any(Inbox.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate token"));

        assertThatThrownBy(() -> service.create("name", null))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(inboxRepository, times(3)).saveAndFlush(any(Inbox.class));
    }

    @Test
    void getsInboxByToken() {
        Inbox inbox = inbox(5L, "abc123");
        when(inboxRepository.findByToken("abc123")).thenReturn(Optional.of(inbox));

        InboxResponse response = service.getByToken("abc123");

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.token()).isEqualTo("abc123");
    }

    @Test
    void failsWhenInboxNotFound() {
        when(inboxRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByToken("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deletesInbox() {
        Inbox inbox = inbox(7L, "tok");
        when(inboxRepository.findByToken("tok")).thenReturn(Optional.of(inbox));

        service.delete("tok");

        verify(inboxRepository).delete(inbox);
    }

    @Test
    void failsToDeleteMissingInbox() {
        when(inboxRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(inboxRepository, never()).delete(any(Inbox.class));
    }

    private Inbox inbox(Long id, String token) {
        Inbox inbox = new Inbox();
        inbox.setId(id);
        inbox.setToken(token);
        inbox.setName("inbox");
        inbox.setActive(true);
        inbox.setCreatedAt(Instant.now());
        return inbox;
    }
}
