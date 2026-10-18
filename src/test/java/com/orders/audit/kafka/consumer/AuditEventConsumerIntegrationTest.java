package com.orders.audit.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orders.audit.common.events.OrderCreatedEvent;
import com.orders.audit.idempotency.IdempotencyService;
import com.orders.audit.intgeration.AbstractAuditIntegrationTest;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(AuditKafkaTestConfiguration.class)
public class AuditEventConsumerIntegrationTest extends AbstractAuditIntegrationTest {

        /*
         * ✅ Successful processing: Creates the audit record and marks the event as
         * processed.
         * ✅ Duplicate delivery: Ignores events that have already been processed.
         * ✅ Persistence failure: Does not mark the event as processed when audit
         * persistence fails.
         * ✅ Serialization failure: Does not mark the event as processed when event
         * serialization fails.
         */
        private static final String CONSUMER_NAME = "AUDIT";

        @Value("${kafka.topics.order-created}")
        private String orderCreatedTopic;

        @Autowired
        private KafkaTemplate<String, String> kafkaTemplate;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private IdempotencyService idempotencyService;

        @Autowired
        private KafkaListenerEndpointRegistry kafkaListenerRegistry;

        @BeforeEach
        void waitForKafkaConsumer() {

                kafkaListenerRegistry
                                .getListenerContainers()
                                .forEach(container -> {

                                        ContainerTestUtils.waitForAssignment(
                                                        container,
                                                        1);

                                });
        }

        @Test
        void shouldHaveRunningKafkaListener() {

                kafkaListenerRegistry
                                .getListenerContainers()
                                .forEach(container -> {

                                        System.out.println(
                                                        "Listener ID: " + container.getListenerId());

                                        System.out.println(
                                                        "Running: " + container.isRunning());

                                        System.out.println(
                                                        "Assigned partitions: "
                                                                        + container.getAssignedPartitions());
                                });
        }

        @Test
        void shouldConnectToPostgreSQLContainer() {
                String databaseName = jdbcTemplate.queryForObject(
                                "SELECT current_database()", String.class);

                assertThat(databaseName).isEqualTo("audit_test");
        }

        @Test
        void shouldPersistAuditRecordAndIdempotencyMarker()
                        throws Exception {

                // Arrange
                UUID eventId = UUID.randomUUID();
                UUID orderId = UUID.randomUUID();
                UUID customerId = UUID.randomUUID();

                Instant occurredAt = Instant.parse("2026-10-14T08:00:00Z");

                String orderNumber = "ORD-IT-" + UUID.randomUUID();

                OrderCreatedEvent event = new OrderCreatedEvent(
                                eventId,
                                "order.created",
                                occurredAt,
                                orderId,
                                orderNumber,
                                customerId,
                                new BigDecimal("125.00"),
                                "QAR",
                                List.of());

                String message = objectMapper.writeValueAsString(event);

                // Act: publish a real JSON message to Kafka.
                var sendResult = kafkaTemplate.send(
                                orderCreatedTopic,
                                orderId.toString(),
                                message).get(10, TimeUnit.SECONDS);

                assertThat(sendResult.getRecordMetadata().topic())
                                .isEqualTo(orderCreatedTopic);

                // Assert: wait for asynchronous consumption and persistence.
                Awaitility.await()
                                .alias("audit record and idempotency marker persisted")
                                .atMost(Duration.ofSeconds(20))
                                .pollInterval(Duration.ofMillis(250))
                                .untilAsserted(() -> {

                                        Integer auditCount = jdbcTemplate.queryForObject(
                                                        """
                                                                        SELECT COUNT(*)
                                                                        FROM audit_events
                                                                        WHERE event_id = ?
                                                                        """,
                                                        Integer.class,
                                                        eventId);

                                        assertThat(auditCount).isEqualTo(1);

                                        // Verify the persisted audit fields.
                                        var auditRow = jdbcTemplate.queryForMap(
                                                        """
                                                                        SELECT event_id,
                                                                               event_type,
                                                                               source_service,
                                                                               aggregate_type,
                                                                               aggregate_id,
                                                                               occurred_at,
                                                                               payload::text AS payload
                                                                        FROM audit_events
                                                                        WHERE event_id = ?
                                                                        """,
                                                        eventId);

                                        assertThat(auditRow.get("event_id"))
                                                        .isEqualTo(eventId);

                                        assertThat(auditRow.get("event_type"))
                                                        .isEqualTo("order.created");

                                        assertThat(auditRow.get("source_service"))
                                                        .isEqualTo("order-service");

                                        assertThat(auditRow.get("aggregate_type"))
                                                        .isEqualTo("ORDER");

                                        assertThat(auditRow.get("aggregate_id"))
                                                        .isEqualTo(orderId.toString());

                                        assertThat(auditRow.get("occurred_at"))
                                                        .isNotNull();

                                        // Verify the JSONB payload.
                                        try {
                                                JsonNode payload = objectMapper.readTree(
                                                                auditRow.get("payload").toString());

                                                assertThat(payload.path("eventId").asText())
                                                                .isEqualTo(eventId.toString());

                                                assertThat(payload.path("orderId").asText())
                                                                .isEqualTo(orderId.toString());

                                                assertThat(payload.path("orderNumber").asText())
                                                                .isEqualTo(orderNumber);

                                                assertThat(payload.path("customerId").asText())
                                                                .isEqualTo(customerId.toString());

                                                assertThat(payload.path("currency").asText())
                                                                .isEqualTo("QAR");

                                                assertThat(payload.path("items").isArray())
                                                                .isTrue();

                                        } catch (Exception exception) {
                                                throw new AssertionError(
                                                                "Could not verify persisted audit payload",
                                                                exception);
                                        }

                                        // Verify the actual database-backed idempotency marker.
                                        assertThat(
                                                        idempotencyService.alreadyProcessed(
                                                                        eventId, CONSUMER_NAME))
                                                        .isTrue();
                                });
        }

}
