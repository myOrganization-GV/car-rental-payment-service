package com.gui.car_rental_payment_service.repositories;

import com.gui.car_rental_payment_service.entities.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, UUID> {
    boolean existsBySagaIdAndMessageType(UUID sagaId, String messageType);
}
