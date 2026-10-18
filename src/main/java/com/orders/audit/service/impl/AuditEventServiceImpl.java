package com.orders.audit.service.impl;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orders.audit.domain.model.AuditEvent;
import com.orders.audit.repository.AuditEventRepository;
import com.orders.audit.service.AuditEventService;

import lombok.RequiredArgsConstructor;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuditEventServiceImpl implements AuditEventService {

        
        
        private final AuditEventRepository auditEventRepository;

        /**
         * Record an audit event.
         *
         * If the event has already been recorded, return the existing record.
         * The database unique constraint remains the final safeguard against
         * concurrent duplicate inserts.
         */
        @Override
        @Transactional
        public AuditEvent recordEvent(AuditEvent auditEvent) {

                Objects.requireNonNull(auditEvent, "auditEvent must not be null");

                UUID eventId = Objects.requireNonNull(auditEvent.getEventId(),
                                "eventId must not be null");

                Optional<AuditEvent> existingEvent = auditEventRepository.findByEventId(eventId);

                if (existingEvent.isPresent()) {
                        return existingEvent.get();
                }

                return auditEventRepository.saveAndFlush(auditEvent);
        }

        @Override
        public Optional<AuditEvent> getByEventId(UUID eventId) {
                Objects.requireNonNull(eventId, "eventId must not be null");

                return auditEventRepository.findByEventId(eventId);
        }

        @Override
        public boolean existsByEventId(UUID eventId) {
                Objects.requireNonNull(eventId, "eventId must not be null");

                return auditEventRepository.existsByEventId(eventId);
        }

        @Override
        public Page<AuditEvent> getByAggregate(String aggregateType,String aggregateId,Pageable pageable) {

                requireText(aggregateType, "aggregateType");
                requireText(aggregateId, "aggregateId");
                requirePageable(pageable);

                return auditEventRepository
                                .findByAggregateTypeAndAggregateIdOrderByOccurredAtDesc(aggregateType,aggregateId,pageable);
        }

        @Override
        public Page<AuditEvent> getByEventType(String eventType,Pageable pageable) {

                requireText(eventType, "eventType");
                requirePageable(pageable);

                return auditEventRepository
                                .findByEventTypeOrderByOccurredAtDesc(eventType,pageable);
        }

        @Override
        public Page<AuditEvent> getBySourceService(String sourceService,Pageable pageable) {

                requireText(sourceService, "sourceService");
                requirePageable(pageable);

                return auditEventRepository
                                .findBySourceServiceOrderByOccurredAtDesc(sourceService,pageable);
        }

        @Override
        public Page<AuditEvent> getByOccurredAtBetween(Instant from,Instant to,Pageable pageable) {

                validateTimeRange(from, to);
                requirePageable(pageable);

                return auditEventRepository
                                .findByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(from,to,pageable);
        }

        @Override
        public Page<AuditEvent> getByAggregateAndOccurredAtBetween(
                        String aggregateType,String aggregateId,Instant from,Instant to,Pageable pageable) {

                requireText(aggregateType, "aggregateType");
                requireText(aggregateId, "aggregateId");
                validateTimeRange(from, to);
                requirePageable(pageable);

                return auditEventRepository
                                .findByAggregateTypeAndAggregateIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
                                                aggregateType,aggregateId,from,to,pageable);
        }

        private static void requireText(String value,String fieldName) {

                if (value == null || value.isBlank()) {
                        throw new IllegalArgumentException(fieldName + " must not be blank");
                }
        }

        private static void requirePageable(Pageable pageable) {
                Objects.requireNonNull(pageable, "pageable must not be null");
        }

        private static void validateTimeRange(Instant from,Instant to) {

                Objects.requireNonNull(from, "from must not be null");
                Objects.requireNonNull(to, "to must not be null");

                if (!from.isBefore(to)) {
                        throw new IllegalArgumentException("'from' must be earlier than 'to'");
                }
        }

}
