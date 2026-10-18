package com.orders.audit.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orders.audit.domain.model.AuditEvent;

public interface AuditEventService {

     /**
     * Persist an audit event.
     *
     * @param auditEvent event to persist
     * @return persisted audit event
     */
    AuditEvent recordEvent(AuditEvent auditEvent);

    /**
     * Retrieve an audit event by its originating event ID.
     */
    Optional<AuditEvent> getByEventId(UUID eventId);

    /**
     * Check whether an event has already been recorded.
     */
    boolean existsByEventId(UUID eventId);

    /**
     * Retrieve the audit history for an aggregate.
     */
    Page<AuditEvent> getByAggregate(String aggregateType,String aggregateId,Pageable pageable);

    /**
     * Search audit events by event type.
     */
    Page<AuditEvent> getByEventType(String eventType,Pageable pageable);

    /**
     * Search audit events by source service.
     */
    Page<AuditEvent> getBySourceService(String sourceService,Pageable pageable);

    /**
     * Search audit events within an occurrence-time range.
     */
    Page<AuditEvent> getByOccurredAtBetween(Instant from,Instant to,Pageable pageable);

    /**
     * Retrieve an aggregate's audit history within a time range.
     */
    Page<AuditEvent> getByAggregateAndOccurredAtBetween(String aggregateType,
            String aggregateId,Instant from,Instant to,Pageable pageable);
}
