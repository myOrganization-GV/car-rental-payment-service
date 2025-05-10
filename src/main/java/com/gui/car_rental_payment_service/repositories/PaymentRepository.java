package com.gui.car_rental_payment_service.repositories;

import com.gui.car_rental_payment_service.entities.MyPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<MyPayment, UUID> {
}
