package com.orders.audit.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orders.audit.common.events.OrderCreatedEvent;
import com.orders.audit.domain.model.AuditEvent;
import com.orders.audit.idempotency.IdempotencyService;
import com.orders.audit.service.AuditEventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventConsumer {

        private static final String CONSUMER_NAME = "AUDIT";
        private static final String SOURCE_SERVICE = "order-service";
        private static final String AGGREGATE_TYPE = "ORDER";

        private final IdempotencyService idempotencyService;
        private final AuditEventService auditEventService;
        private final ObjectMapper objectMapper;

        @KafkaListener(topics = "${kafka.topics.order-created}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaAuditListenerContainerFactory")
        @Transactional
        public void consume(OrderCreatedEvent event) {

                log.info(
                                "Received OrderCreatedEvent: eventId={}, orderId={}",
                                event.eventId(),
                                event.orderId());

                if (idempotencyService.alreadyProcessed(
                                event.eventId(), CONSUMER_NAME)) {

                        log.info("Duplicate audit event ignored: eventId={}", event.eventId());

                        return;
                }

                createAuditRecord(event);

                idempotencyService.markProcessed(event.eventId(), CONSUMER_NAME);

                log.info(
                                "Audit record created successfully: eventId={}, orderId={}",
                                event.eventId(),
                                event.orderId());
        }

        
        private void createAuditRecord(OrderCreatedEvent event) {

                AuditEvent auditEvent = new AuditEvent(
                                event.eventId(),
                                event.eventType(),
                                SOURCE_SERVICE,
                                AGGREGATE_TYPE,
                                event.orderId().toString(),
                                null,
                                event.occurredAt(),
                                objectMapper.valueToTree(event));

                auditEventService.recordEvent(auditEvent);

                log.debug(
                                "Persisted audit record: eventId={}, orderId={}",
                                event.eventId(),
                                event.orderId());
        }
}
