package com.orders.audit.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.orders.audit.domain.model.AuditEvent;
import com.orders.audit.service.AuditEventService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/audit-events")
@RequiredArgsConstructor
public class AuditEventController {

    private final AuditEventService auditEventService;

    // ==========================================
    // GET AUDIT EVENT BY ID
    // ==========================================

    @GetMapping("/{eventId}")
    public ResponseEntity<AuditEvent> getByEventId(
            @PathVariable UUID eventId) {

        return auditEventService.getByEventId(eventId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ==========================================
    // CHECK EVENT EXISTENCE
    // ==========================================

    @GetMapping("/{eventId}/exists")
    public ResponseEntity<Boolean> existsByEventId(
            @PathVariable UUID eventId) {

        return ResponseEntity.ok(
                auditEventService.existsByEventId(eventId));
    }

    // ==========================================
    // GET EVENTS BY AGGREGATE
    // ==========================================

    @GetMapping("/aggregate/{aggregateType}/{aggregateId}")
    public ResponseEntity<Page<AuditEvent>> getByAggregate(
            @PathVariable String aggregateType,
            @PathVariable String aggregateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = createPageable(page, size);

        return ResponseEntity.ok(
                auditEventService.getByAggregate(
                        aggregateType,
                        aggregateId,
                        pageable));
    }

    // ==========================================
    // GET EVENTS BY EVENT TYPE
    // ==========================================

    @GetMapping("/type/{eventType}")
    public ResponseEntity<Page<AuditEvent>> getByEventType(
            @PathVariable String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = createPageable(page, size);

        return ResponseEntity.ok(
                auditEventService.getByEventType(
                        eventType,
                        pageable));
    }

    // ==========================================
    // GET EVENTS BY SOURCE SERVICE
    // ==========================================

    @GetMapping("/source/{sourceService}")
    public ResponseEntity<Page<AuditEvent>> getBySourceService(
            @PathVariable String sourceService,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = createPageable(page, size);

        return ResponseEntity.ok(
                auditEventService.getBySourceService(
                        sourceService,
                        pageable));
    }

    // ==========================================
    // GET EVENTS WITHIN TIME RANGE
    // ==========================================

    @GetMapping("/search")
    public ResponseEntity<Page<AuditEvent>> getByOccurredAtBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = createPageable(page, size);

        return ResponseEntity.ok(
                auditEventService.getByOccurredAtBetween(
                        from,
                        to,
                        pageable));
    }

    // ==========================================
    // GET AGGREGATE EVENTS WITHIN TIME RANGE
    // ==========================================

    @GetMapping("/aggregate/{aggregateType}/{aggregateId}/search")
    public ResponseEntity<Page<AuditEvent>> getByAggregateAndOccurredAtBetween(

            @PathVariable String aggregateType,
            @PathVariable String aggregateId,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = createPageable(page, size);

        return ResponseEntity.ok(
                auditEventService.getByAggregateAndOccurredAtBetween(
                        aggregateType,
                        aggregateId,
                        from,
                        to,
                        pageable));
    }

    // ==========================================
    // PAGINATION VALIDATION
    // ==========================================

    private Pageable createPageable(int page, int size) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "page must be greater than or equal to 0");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "size must be between 1 and 100");
        }

        return PageRequest.of(page, size);
    }
}
