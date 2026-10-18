package com.orders.audit.idempotency;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;




@Entity
@Table(
    name = "audit_processed_events",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_audit_processed_event_consumer",
            columnNames = {"event_id","consumer_name"}
        )
    }
)
@Getter 
@Builder
public class ProcessedEvent {
    //We need a table that remembers which events have already been processed.

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id",nullable = false)
    private UUID eventId;

    @Column(name = "consumer_name",nullable = false,length = 100)
    private String consumerName;  // PAYMENT , INVENTORY , AUDIT

    @Column(name = "processed_at",nullable = false)
    private Instant processedAt;

}
