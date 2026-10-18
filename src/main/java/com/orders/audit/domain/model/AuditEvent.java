package com.orders.audit.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "audit_events",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_audit_events_event_id",
            columnNames = "event_id"
        )
    }
)
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 150)
    private String eventType;

    @Column(name = "source_service", nullable = false, updatable = false, length = 100)
    private String sourceService;

    @Column(name = "aggregate_type", nullable = false, updatable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, updatable = false, length = 100)
    private String aggregateId;

    @Column(name = "correlation_id", updatable = false, length = 100)
    private String correlationId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    /**
     * Required by JPA.
     */
    protected AuditEvent() {}

    public AuditEvent(
            UUID eventId,
            String eventType,
            String sourceService,
            String aggregateType,
            String aggregateId,
            String correlationId,
            Instant occurredAt,
            JsonNode payload) {

        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.eventType = requireText(eventType, "eventType");
        this.sourceService = requireText(sourceService, "sourceService");
        this.aggregateType = requireText(aggregateType, "aggregateType");
        this.aggregateId = requireText(aggregateId, "aggregateId");
        this.correlationId = correlationId;
        this.occurredAt = Objects.requireNonNull(
                occurredAt, "occurredAt must not be null");
        this.payload = Objects.requireNonNull(
                payload, "payload must not be null").deepCopy();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank");
        }
        return value;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSourceService() {
        return sourceService;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public JsonNode getPayload() {
        return payload == null ? null : payload.deepCopy();
    }
}
