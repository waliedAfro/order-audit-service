package com.orders.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.domain.Sort;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orders.audit.domain.model.AuditEvent;
import com.orders.audit.intgeration.AbstractAuditIntegrationTest;
import com.orders.audit.repository.AuditEventRepository;

@SpringBootTest
@Transactional
public class AuditEventServiceImplTest extends AbstractAuditIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID eventId;
    private UUID orderId;
    private Instant occurredAt;
    private ObjectNode payload;
    private AuditEvent auditEvent;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        occurredAt = Instant.parse("2026-10-14T08:00:00Z");

        payload = objectMapper.createObjectNode();
        payload.put("eventId", eventId.toString());
        payload.put("eventType", "order.created");
        payload.put("orderId", orderId.toString());
        payload.put("totalAmount", 250.00);

        auditEvent = new AuditEvent(
                eventId,
                "order.created",
                "order-service",
                "ORDER",
                orderId.toString(),
                null,
                occurredAt,
                payload);
    }

    @Test
    void shouldConnectToPostgreSQLContainer() {
        String databaseName = jdbcTemplate.queryForObject(
                "SELECT current_database()", String.class);

        assertThat(databaseName).isEqualTo("audit_test");
    }

    @Test
    void shouldPersistAuditEvent() {
        AuditEvent saved = auditEventService.recordEvent(auditEvent);

        assertNotNull(saved);
        assertEquals(eventId, saved.getEventId());
        assertEquals("order.created", saved.getEventType());
        assertEquals("order-service", saved.getSourceService());
        assertEquals("ORDER", saved.getAggregateType());
        assertEquals(orderId.toString(), saved.getAggregateId());
        assertEquals(occurredAt, saved.getOccurredAt());

        assertTrue(auditEventRepository.existsByEventId(eventId));

        AuditEvent reloaded = auditEventRepository
                .findByEventId(eventId)
                .orElseThrow();

        assertEquals(eventId, reloaded.getEventId());
        assertEquals(payload, reloaded.getPayload());
        assertNotNull(reloaded.getRecordedAt());
    }

    @Test
    void shouldReturnExistingEventWhenEventIdAlreadyExists() {
        AuditEvent first = auditEventService.recordEvent(auditEvent);

        // Same event ID, but a different payload.
        ObjectNode duplicatePayload = objectMapper.createObjectNode();
        duplicatePayload.put("eventId", eventId.toString());
        duplicatePayload.put("eventType", "order.created");
        duplicatePayload.put("message", "duplicate delivery");

        AuditEvent duplicate = new AuditEvent(
                eventId,
                "order.created",
                "order-service",
                "ORDER",
                orderId.toString(),
                null,
                occurredAt,
                duplicatePayload);

        AuditEvent result = auditEventService.recordEvent(duplicate);

        assertNotNull(result);
        assertEquals(first.getEventId(), result.getEventId());
        assertEquals(first.getPayload(), result.getPayload());

        assertEquals(
                1,
                auditEventRepository.findAll().stream()
                        .filter(e -> eventId.equals(e.getEventId()))
                        .count());
    }

    @Test
    void shouldConfirmEventExists() {
        auditEventService.recordEvent(auditEvent);

        assertTrue(auditEventService.existsByEventId(eventId));
        assertFalse(auditEventService.existsByEventId(UUID.randomUUID()));
    }

    @Test
    void shouldRetrieveEventByEventId() {
        auditEventService.recordEvent(auditEvent);

        AuditEvent found = auditEventService.getByEventId(eventId).get();

        assertNotNull(found);
        assertEquals(eventId, found.getEventId());
        assertEquals(orderId.toString(), found.getAggregateId());
    }

    // *******************************************************************************
    // */

    @Test
    void shouldRetrieveEventsByAggregate() {

        UUID aggregateId = UUID.randomUUID();

        AuditEvent first = createEvent(
                UUID.randomUUID(),
                "order.created",
                "order-service",
                "ORDER",
                aggregateId.toString(),
                Instant.parse("2026-10-14T08:00:00Z"));

        AuditEvent second = createEvent(
                UUID.randomUUID(),
                "order.updated",
                "order-service",
                "ORDER",
                aggregateId.toString(),
                Instant.parse("2026-10-14T09:00:00Z"));

        AuditEvent unrelated = createEvent(
                UUID.randomUUID(),
                "order.created",
                "order-service",
                "ORDER",
                UUID.randomUUID().toString(),
                Instant.parse("2026-10-14T10:00:00Z"));

        auditEventService.recordEvent(first);
        auditEventService.recordEvent(second);
        auditEventService.recordEvent(unrelated);

        Page<AuditEvent> result = auditEventService.getByAggregate(
                "ORDER", aggregateId.toString(), PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
                .allMatch(e -> aggregateId.toString().equals(e.getAggregateId())));
    }

    @Test
    void shouldRetrieveEventsByEventType() {
        auditEventService.recordEvent(createEvent(
                UUID.randomUUID(), "order.created", "order-service",
                "ORDER", UUID.randomUUID().toString(),
                Instant.parse("2026-10-14T08:00:00Z")));

        auditEventService.recordEvent(createEvent(
                UUID.randomUUID(), "order.created", "payment-service",
                "PAYMENT", UUID.randomUUID().toString(),
                Instant.parse("2026-10-14T09:00:00Z")));

        auditEventService.recordEvent(createEvent(
                UUID.randomUUID(), "payment.completed", "payment-service",
                "PAYMENT", UUID.randomUUID().toString(),
                Instant.parse("2026-10-14T10:00:00Z")));

        Page<AuditEvent> result = auditEventService.getByEventType(
                "order.created", PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
                .allMatch(e -> "order.created".equals(e.getEventType())));
    }

    @Test
    void shouldRetrieveEventsBySourceService() {
        auditEventService.recordEvent(createEvent(
                UUID.randomUUID(), "order.created", "order-service",
                "ORDER", UUID.randomUUID().toString(),
                Instant.parse("2026-10-14T08:00:00Z")));

        auditEventService.recordEvent(createEvent(
                UUID.randomUUID(), "order.updated", "order-service",
                "ORDER", UUID.randomUUID().toString(),
                Instant.parse("2026-10-14T09:00:00Z")));

        auditEventService.recordEvent(createEvent(
                UUID.randomUUID(), "payment.completed", "payment-service",
                "PAYMENT", UUID.randomUUID().toString(),
                Instant.parse("2026-10-14T10:00:00Z")));

        Page<AuditEvent> result = auditEventService.getBySourceService(
                "order-service", PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
                .allMatch(e -> "order-service".equals(e.getSourceService())));
    }

    @Test
    void shouldRetrieveEventsWithinOccurredAtRange() {
        auditEventService.recordEvent(createEvent(
                UUID.randomUUID(), "order.created", "order-service",
                "ORDER", UUID.randomUUID().toString(),
                Instant.parse("2026-10-14T08:00:00Z")));

        auditEventService.recordEvent(createEvent(
                UUID.randomUUID(), "order.updated", "order-service",
                "ORDER", UUID.randomUUID().toString(),
                Instant.parse("2026-10-14T09:00:00Z")));

        auditEventService.recordEvent(createEvent(
                UUID.randomUUID(), "payment.completed", "payment-service",
                "PAYMENT", UUID.randomUUID().toString(),
                Instant.parse("2026-10-14T10:00:00Z")));

        Page<AuditEvent> result = auditEventService.getByOccurredAtBetween(
                Instant.parse("2026-10-14T08:30:00Z"),
                Instant.parse("2026-10-14T09:30:00Z"),
                PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("order.updated", result.getContent().get(0).getEventType());
    }

    @Test
    void shouldRetrieveAggregateEventsWithinOccurredAtRange() {
        String aggregateId = UUID.randomUUID().toString();

        auditEventService.recordEvent(createEvent(
                UUID.randomUUID(), "order.created", "order-service",
                "ORDER", aggregateId,
                Instant.parse("2026-10-14T08:00:00Z")));

        auditEventService.recordEvent(createEvent(
                UUID.randomUUID(), "order.updated", "order-service",
                "ORDER", aggregateId,
                Instant.parse("2026-10-14T09:00:00Z")));

        auditEventService.recordEvent(createEvent(
                UUID.randomUUID(), "order.cancelled", "order-service",
                "ORDER", aggregateId,
                Instant.parse("2026-10-14T10:00:00Z")));

        Page<AuditEvent> result = auditEventService.getByAggregateAndOccurredAtBetween(
                "ORDER",
                aggregateId,
                Instant.parse("2026-10-14T08:30:00Z"),
                Instant.parse("2026-10-14T09:30:00Z"),
                PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("order.updated", result.getContent().get(0).getEventType());
    }

    @Test
    void shouldPaginateEventsByEventType() {
        for (int i = 0; i < 5; i++) {
            auditEventService.recordEvent(createEvent(
                    UUID.randomUUID(),
                    "pagination.test",
                    "order-service",
                    "ORDER",
                    UUID.randomUUID().toString(),
                    Instant.parse("2026-10-14T08:00:00Z").plusSeconds(i)));
        }

        Page<AuditEvent> firstPage = auditEventService.getByEventType(
                "pagination.test", PageRequest.of(0, 2));

        Page<AuditEvent> secondPage = auditEventService.getByEventType(
                "pagination.test", PageRequest.of(1, 2));

        assertEquals(2, firstPage.getContent().size());
        assertEquals(2, secondPage.getContent().size());
        assertEquals(5, firstPage.getTotalElements());
        assertEquals(3, firstPage.getTotalPages());

        @SuppressWarnings("null")
        List<UUID> firstPageIds = firstPage.getContent().stream()
                .map(AuditEvent::getEventId)
                .toList();

        @SuppressWarnings("null")
        List<UUID> secondPageIds = secondPage.getContent().stream()
                .map(AuditEvent::getEventId)
                .toList();

        assertTrue(firstPageIds.stream().noneMatch(secondPageIds::contains));
    }

    @Test
    void shouldReturnEmptyPageWhenNoEventsMatch() {
        Page<AuditEvent> result = auditEventService.getByEventType(
                "event.type.that.does.not.exist",
                PageRequest.of(0, 10));

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    private AuditEvent createEvent(
            UUID eventId,
            String eventType,
            String sourceService,
            String aggregateType,
            String aggregateId,
            Instant occurredAt) {

        var payload = objectMapper.createObjectNode();
        payload.put("eventId", eventId.toString());
        payload.put("eventType", eventType);
        payload.put("aggregateId", aggregateId);

        return new AuditEvent(
                eventId,
                eventType,
                sourceService,
                aggregateType,
                aggregateId,
                null,
                occurredAt,
                payload);
    }

/*
    1.Retrieve events by aggregate type and ID.
    2.Retrieve events by event type.
    3.Retrieve events by source service.
    4.Retrieve events within a timestamp range, including aggregate-specific ranges.
    5.Verify pagination, total elements, total pages, and no overlap between pages.
 */
   
}
