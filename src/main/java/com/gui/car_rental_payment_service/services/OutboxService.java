package com.gui.car_rental_payment_service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gui.car_rental_payment_service.entities.OutboxEvent;
import com.gui.car_rental_payment_service.repositories.OutboxEventRepository;
import org.springframework.stereotype.Service;

/**
 * Write side of the transactional outbox. {@link #saveEvent} serializes the event and stores it as an
 * {@link OutboxEvent} row. It is called from within a business transaction, so the row commits atomically
 * with the business change; {@code OutboxRelay} publishes it to Kafka afterwards.
 */
@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void saveEvent(String topic, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxEventRepository.save(new OutboxEvent(topic, event.getClass().getName(), payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event of type "
                    + event.getClass().getName(), e);
        }
    }
}
