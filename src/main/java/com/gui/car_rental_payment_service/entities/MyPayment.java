package com.gui.car_rental_payment_service.entities;

import com.gui.car_rental_payment_service.enums.MyPaymentMethod;
import com.gui.car_rental_payment_service.enums.MyPaymentStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class MyPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID paymentId;
    private UUID bookingId;
    private UUID carId;

    @Column(name = "mercado_pago_id")
    private String mercadoPagoId;

    private UUID sagaId;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private MyPaymentStatus myPaymentStatus;


    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private MyPaymentMethod myPaymentMethod;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private String mockQrCode;

    @Column(columnDefinition = "TEXT")
    private String mockQrCodeBase64;
    public MyPayment(UUID paymentId, UUID bookingId, UUID carId, String mercadoPagoId, UUID sagaId, BigDecimal amount, MyPaymentStatus myPaymentStatus, MyPaymentMethod myPaymentMethod, LocalDateTime createdAt, LocalDateTime updatedAt, String mockQrCode , String mockQrCodeBase64) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.carId = carId;
        this.mercadoPagoId = mercadoPagoId;
        this.sagaId = sagaId;
        this.amount = amount;
        this.myPaymentStatus = myPaymentStatus;
        this.myPaymentMethod = myPaymentMethod;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.mockQrCode = mockQrCode;
        this.mockQrCodeBase64 = mockQrCodeBase64;
    }

    public String getMockQrCode() {
        return mockQrCode;
    }

    public void setMockQrCode(String mockQrCode) {
        this.mockQrCode = mockQrCode;
    }

    public String getMockQrCodeBase64() {
        return mockQrCodeBase64;
    }

    public void setMockQrCodeBase64(String mockQrCodeBase64) {
        this.mockQrCodeBase64 = mockQrCodeBase64;
    }

    public UUID getSagaId() {
        return sagaId;
    }

    public void setSagaId(UUID sagaId) {
        this.sagaId = sagaId;
    }

    public MyPayment() {
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public void setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
    }

    public UUID getCarId() {
        return carId;
    }

    public void setCarId(UUID carId) {
        this.carId = carId;
    }

    public String getMercadoPagoId() {
        return mercadoPagoId;
    }

    public void setMercadoPagoId(String mercadoPagoId) {
        this.mercadoPagoId = mercadoPagoId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public MyPaymentStatus getMyPaymentStatus() {
        return myPaymentStatus;
    }

    public void setMyPaymentStatus(MyPaymentStatus myPaymentStatus) {
        this.myPaymentStatus = myPaymentStatus;
    }

    public MyPaymentMethod getMyPaymentMethod() {
        return myPaymentMethod;
    }

    public void setMyPaymentMethod(MyPaymentMethod myPaymentMethod) {
        this.myPaymentMethod = myPaymentMethod;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}