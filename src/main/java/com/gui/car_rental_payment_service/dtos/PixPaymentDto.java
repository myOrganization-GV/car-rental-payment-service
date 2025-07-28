package com.gui.car_rental_payment_service.dtos;

import java.util.UUID;

public class PixPaymentDto {

    private UUID sagaId;
    private String qrCode;
    private String qrCodeBase64;

    public UUID getSagaId() {
        return sagaId;
    }

    public void setSagaId(UUID sagaId) {
        this.sagaId = sagaId;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getQrCodeBase64() {
        return qrCodeBase64;
    }

    public void setQrCodeBase64(String qrCodeBase64) {
        this.qrCodeBase64 = qrCodeBase64;
    }

    public PixPaymentDto(UUID sagaId, String qrCode, String qrCodeBase64) {
        this.sagaId = sagaId;
        this.qrCode = qrCode;
        this.qrCodeBase64 = qrCodeBase64;
    }

    public PixPaymentDto() {
    }
}
