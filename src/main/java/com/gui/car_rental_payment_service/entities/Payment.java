package com.gui.car_rental_payment_service.entities;

import com.gui.car_rental_payment_service.enums.PaymentMethod;
import com.gui.car_rental_payment_service.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID paymentId;
    @NotNull
    private UUID bookingId;
    @NotNull
    private UUID carId;

    private UUID userId;

    private String userName;

    private BigDecimal amount;

    private PaymentStatus paymentStatus;

    private PaymentMethod paymentMethod;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}