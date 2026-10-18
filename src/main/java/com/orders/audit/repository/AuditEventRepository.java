package com.orders.audit.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.orders.audit.domain.model.AuditEvent;

@Repository 
public interface AuditEventRepository extends JpaRepository<AuditEvent , UUID>, 
        JpaSpecificationExecutor<AuditEvent>{
    
     /**
     * Find an audit record by the originating event ID.
     */
    Optional<AuditEvent> findByEventId(UUID eventId);

    /**
     * Check whether an event has already been recorded.
     */
    boolean existsByEventId(UUID eventId);

    /**
     * Retrieve all events for a business aggregate, such as an order.
     */
    Page<AuditEvent> findByAggregateTypeAndAggregateIdOrderByOccurredAtDesc(
            String aggregateType,String aggregateId,Pageable pageable);

    /**
     * Find audit events by type.
     */
    Page<AuditEvent> findByEventTypeOrderByOccurredAtDesc(
            String eventType,Pageable pageable);

    /**
     * Find audit events produced by a particular service.
     */
    Page<AuditEvent> findBySourceServiceOrderByOccurredAtDesc(
            String sourceService,Pageable pageable);

    /**
     * Find audit events within an occurrence-time range.
     */
    Page<AuditEvent> findByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
            Instant from,Instant to,Pageable pageable);

    /**
     * Retrieve the complete audit history for an aggregate.
     */
    Page<AuditEvent> findByAggregateTypeAndAggregateIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
            String aggregateType,String aggregateId,Instant from,Instant to,Pageable pageable);


}
