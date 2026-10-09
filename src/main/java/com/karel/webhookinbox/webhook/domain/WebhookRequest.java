package com.karel.webhookinbox.webhook.domain;

import com.karel.webhookinbox.inbox.domain.Inbox;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "webhook_request")
@Getter
@Setter
public class WebhookRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inbox_id", nullable = false)
    private Inbox inbox;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, columnDefinition = "text")
    private String path;

    @Column(name = "query_string", columnDefinition = "text")
    private String queryString;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, List<String>> headers;

    @Column(name = "content_type")
    private String contentType;

    @Column(columnDefinition = "text")
    private String body;

    @Column(name = "body_size", nullable = false)
    private int bodySize;

    @Column(name = "source_ip", length = 64)
    private String sourceIp;

    @Generated(event = EventType.INSERT)
    @Column(name = "received_at", nullable = false, insertable = false, updatable = false)
    private Instant receivedAt;
}
