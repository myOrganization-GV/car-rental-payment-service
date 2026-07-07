package com.gui.car_rental_payment_service.repositories;

import com.gui.car_rental_payment_service.entities.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
