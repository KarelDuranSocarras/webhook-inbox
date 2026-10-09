package com.karel.webhookinbox.inbox.application;

import com.karel.webhookinbox.common.error.ResourceNotFoundException;
import com.karel.webhookinbox.common.error.ValidationException;
import com.karel.webhookinbox.common.util.TokenGenerator;
import com.karel.webhookinbox.config.WebhookInboxProperties;
import com.karel.webhookinbox.inbox.domain.Inbox;
import com.karel.webhookinbox.inbox.domain.InboxRepository;
import com.karel.webhookinbox.inbox.dto.InboxResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InboxService {

    private static final int NAME_MAX_LENGTH = 120;
    private static final int DESCRIPTION_MAX_LENGTH = 500;
    private static final int TOKEN_RETRIES = 3;

    private final InboxRepository inboxRepository;
    private final TokenGenerator tokenGenerator;
    private final WebhookInboxProperties properties;

    public InboxResponse create(String name, String description) {
        String normalizedName = validateName(name);
        String normalizedDescription = validateDescription(description);

        for (int attempt = 1; attempt <= TOKEN_RETRIES; attempt++) {
            Inbox inbox = new Inbox();
            inbox.setToken(tokenGenerator.generate(properties.tokenLength()));
            inbox.setName(normalizedName);
            inbox.setDescription(normalizedDescription);
            inbox.setActive(true);
            try {
                Inbox saved = inboxRepository.saveAndFlush(inbox);
                return InboxResponse.from(saved);
            } catch (DataIntegrityViolationException ex) {
                log.warn("Inbox token collision on attempt {}/{}", attempt, TOKEN_RETRIES);
            }
        }
        throw new DataIntegrityViolationException(
                "Could not generate a unique inbox token after " + TOKEN_RETRIES + " attempts");
    }

    @Transactional(readOnly = true)
    public List<InboxResponse> list() {
        return inboxRepository.findAll().stream().map(InboxResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public InboxResponse getByToken(String token) {
        return InboxResponse.from(requireInbox(token));
    }

    @Transactional
    public void delete(String token) {
        inboxRepository.delete(requireInbox(token));
    }

    private Inbox requireInbox(String token) {
        return inboxRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Inbox not found: " + token));
    }

    private String validateName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException("Inbox name is required");
        }
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw new ValidationException("Inbox name must be at most " + NAME_MAX_LENGTH + " characters");
        }
        return trimmed;
    }

    private String validateDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        if (trimmed.length() > DESCRIPTION_MAX_LENGTH) {
            throw new ValidationException(
                    "Inbox description must be at most " + DESCRIPTION_MAX_LENGTH + " characters");
        }
        return trimmed.isEmpty() ? null : trimmed;
    }
}
