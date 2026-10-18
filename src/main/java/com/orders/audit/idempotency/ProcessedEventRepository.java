package com.orders.audit.idempotency;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository 
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent,UUID> {

       boolean existsByEventIdAndConsumerName(UUID eventId,String consumerName);


}
