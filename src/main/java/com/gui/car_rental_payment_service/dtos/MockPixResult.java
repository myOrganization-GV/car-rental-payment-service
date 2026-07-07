package com.gui.car_rental_payment_service.dtos;

public class MockPixResult {
    private String qrCodeBase64;
    private String qrCode;

    public MockPixResult() {
    }

    public MockPixResult(String qrCodeBase64, String qrCode) {
        this.qrCodeBase64 = qrCodeBase64;
        this.qrCode = qrCode;
    }

    public String getQrCodeBase64() {
        return qrCodeBase64;
    }

    public void setQrCodeBase64(String qrCodeBase64) {
        this.qrCodeBase64 = qrCodeBase64;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }
}
