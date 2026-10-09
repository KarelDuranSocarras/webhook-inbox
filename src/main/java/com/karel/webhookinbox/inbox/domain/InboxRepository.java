package com.karel.webhookinbox.inbox.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxRepository extends JpaRepository<Inbox, Long> {

    Optional<Inbox> findByToken(String token);
}
