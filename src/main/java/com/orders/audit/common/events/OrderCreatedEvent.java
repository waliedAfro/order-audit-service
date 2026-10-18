package com.orders.audit.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        
        String eventType,

        Instant occurredAt,

        UUID orderId,

        String orderNumber,

        UUID customerId,

        BigDecimal totalAmount,

        String currency ,
        
        List<OrderCreatedItem> items

) {}
