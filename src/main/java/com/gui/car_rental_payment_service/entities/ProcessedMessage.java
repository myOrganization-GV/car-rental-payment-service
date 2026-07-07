package com.gui.car_rental_payment_service.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inbox record for the Idempotent Consumer pattern. One row per successfully handled saga command,
 * keyed by (sagaId, messageType). A redelivered command whose key already exists is skipped.
 */
@Entity
@Table(name = "processed_messages",
        uniqueConstraints = @UniqueConstraint(columnNames = {"saga_id", "message_type"}))
public class ProcessedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "saga_id", nullable = false)
    private UUID sagaId;

    @Column(name = "message_type", nullable = false)
    private String messageType;

    @CreationTimestamp
    @Column(name = "processed_at", updatable = false)
    private LocalDateTime processedAt;

    public ProcessedMessage() {
    }

    public ProcessedMessage(UUID sagaId, String messageType) {
        this.sagaId = sagaId;
        this.messageType = messageType;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSagaId() {
        return sagaId;
    }

    public String getMessageType() {
        return messageType;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}
