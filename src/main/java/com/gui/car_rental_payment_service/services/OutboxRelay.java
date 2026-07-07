package com.gui.car_rental_payment_service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gui.car_rental_payment_service.entities.OutboxEvent;
import com.gui.car_rental_payment_service.repositories.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Relay side of the transactional outbox. Polls for unpublished {@link OutboxEvent} rows and publishes them
 * to Kafka, marking each published only after the broker acknowledges. This gives at-least-once delivery;
 * duplicates on relay restart are absorbed by the idempotent consumers (processed_messages).
 */
@Component
public class OutboxRelay {

    private static final Logger logger = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxRelay(OutboxEventRepository outboxEventRepository,
                       KafkaTemplate<String, Object> kafkaTemplate,
                       ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.delay-ms:1000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEvent event : pending) {
            try {
                Object payload = objectMapper.readValue(event.getPayload(), Class.forName(event.getEventType()));
                kafkaTemplate.send(event.getTopic(), payload).get();
                event.markPublished();
                logger.info("Relayed outbox event {} ({}) to {}",
                        event.getId(), event.getEventType(), event.getTopic());
            } catch (Exception ex) {
                logger.error("Failed to relay outbox event {}: {}", event.getId(), ex.getMessage());
            }
        }
    }
}
