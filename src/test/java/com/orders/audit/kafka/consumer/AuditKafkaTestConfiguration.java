package com.orders.audit.kafka.consumer;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.beans.factory.annotation.Value;

@TestConfiguration(proxyBeanMethods = false)
public class AuditKafkaTestConfiguration {

    @Bean
    NewTopic orderCreatedTestTopic(@Value("${kafka.topics.order-created}")  String topic) {
        return TopicBuilder.name(topic)
                .partitions(1)
                .replicas(1)
                 .config("retention.ms", "60000")  // optional: keep the test broker tidy
                .build();
    }
}
