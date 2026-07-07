package com.gui.car_rental_payment_service.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transactional outbox row. Written in the same DB transaction as the business change, then relayed to
 * Kafka by {@code OutboxRelay}. Guarantees the event is published (at least once) after — and only if —
 * the transaction commits.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String topic;

    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private boolean published = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public OutboxEvent() {
    }

    public OutboxEvent(String topic, String eventType, String payload) {
        this.topic = topic;
        this.eventType = eventType;
        this.payload = payload;
    }

    public void markPublished() {
        this.published = true;
        this.publishedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public boolean isPublished() {
        return published;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
}
