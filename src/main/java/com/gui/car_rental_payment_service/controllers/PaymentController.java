package com.gui.car_rental_payment_service.controllers;

import com.gui.car_rental_payment_service.dtos.PixPaymentDto;
import com.gui.car_rental_payment_service.services.PaymentService;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{sagaId}")
    public ResponseEntity<?> getPixData(@PathVariable UUID sagaId) throws MPException, MPApiException {
        PixPaymentDto pixPaymentDto = paymentService.getPixPaymentDataDto(sagaId);
        if(pixPaymentDto == null){
            System.out.println("Payment with sagaId " + sagaId + " not found, returning 404.");
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", "Payment with sagaId " + sagaId + " not found.");
            return new ResponseEntity<>(errorBody, HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(pixPaymentDto);
    }



}
