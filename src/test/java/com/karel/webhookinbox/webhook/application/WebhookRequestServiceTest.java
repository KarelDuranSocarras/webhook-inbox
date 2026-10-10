package com.karel.webhookinbox.webhook.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.karel.webhookinbox.common.error.ResourceNotFoundException;
import com.karel.webhookinbox.config.WebhookInboxProperties;
import com.karel.webhookinbox.config.WebhookInboxProperties.RateLimit;
import com.karel.webhookinbox.webhook.domain.WebhookRequest;
import com.karel.webhookinbox.webhook.domain.WebhookRequestRepository;
import com.karel.webhookinbox.webhook.dto.WebhookRequestDetail;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class WebhookRequestServiceTest {

    @Mock
    private WebhookRequestRepository webhookRequestRepository;

    private WebhookRequestService service;

    @BeforeEach
    void setUp() {
        WebhookInboxProperties properties =
                new WebhookInboxProperties(1_048_576L, 24, 25, 7, new RateLimit(true, 100), null, true, "0 0 * * * *");
        service = new WebhookRequestService(webhookRequestRepository, properties);
    }

    @Test
    void uppercasesMethodAndPassesFilters() {
        when(webhookRequestRepository.search(eq(1L), eq("POST"), eq("stripe"), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.list(1L, "post", "stripe", 0, 10);

        verify(webhookRequestRepository).search(eq(1L), eq("POST"), eq("stripe"), any(Pageable.class));
    }

    @Test
    void passesNullFiltersWhenBlank() {
        when(webhookRequestRepository.search(eq(1L), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.list(1L, "   ", "   ", 0, 10);

        verify(webhookRequestRepository).search(eq(1L), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void capsPageSizeAt100AndForcesReceivedAtDescending() {
        when(webhookRequestRepository.search(anyLong(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.list(1L, null, null, 0, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(webhookRequestRepository).search(eq(1L), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "receivedAt"));
    }

    @Test
    void usesConfiguredDefaultWhenSizeNotExplicitlySet() {
        when(webhookRequestRepository.search(anyLong(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.list(1L, null, null, 0, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(webhookRequestRepository).search(eq(1L), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(25);
    }

    @Test
    void neverAllowsUnboundedSizeOnNegativeInput() {
        when(webhookRequestRepository.search(anyLong(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.list(1L, null, null, 0, -1);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(webhookRequestRepository).search(eq(1L), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(25);
    }

    @Test
    void clampsNegativePageToZero() {
        when(webhookRequestRepository.search(anyLong(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.list(1L, null, null, -5, 10);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(webhookRequestRepository).search(eq(1L), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
    }

    @Test
    void getsRequestScopedToInbox() {
        WebhookRequest request = new WebhookRequest();
        request.setId(9L);
        request.setMethod("POST");
        when(webhookRequestRepository.findByIdAndInboxId(9L, 1L)).thenReturn(Optional.of(request));

        WebhookRequestDetail detail = service.get(1L, 9L);

        assertThat(detail.id()).isEqualTo(9L);
    }

    @Test
    void failsWhenRequestNotInInbox() {
        when(webhookRequestRepository.findByIdAndInboxId(9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(1L, 9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void clearsInboxWithBulkDelete() {
        when(webhookRequestRepository.deleteByInboxId(1L)).thenReturn(3);

        assertThat(service.clear(1L)).isEqualTo(3);

        verify(webhookRequestRepository).deleteByInboxId(1L);
    }
}
