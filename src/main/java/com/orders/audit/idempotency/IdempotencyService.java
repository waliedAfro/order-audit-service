package com.orders.audit.idempotency;


import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class IdempotencyService {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional(readOnly = true)
    public boolean alreadyProcessed(UUID eventId,String consumerName) {

        validate(eventId, consumerName);

        return processedEventRepository
                .existsByEventIdAndConsumerName(
                        eventId, consumerName);
    }

    @Transactional
    public void markProcessed(UUID eventId,String consumerName) {

        validate(eventId, consumerName);

        processedEventRepository.save(
                ProcessedEvent.builder()
                        .eventId(eventId)
                        .consumerName(consumerName)
                        .processedAt(Instant.now())
                        .build()
        );
    }

    private static void validate(UUID eventId,String consumerName) {

        Objects.requireNonNull(eventId, "eventId must not be null");

        if (consumerName == null || consumerName.isBlank()) {
            throw new IllegalArgumentException(
                    "consumerName must not be blank");
        }
    }
}
