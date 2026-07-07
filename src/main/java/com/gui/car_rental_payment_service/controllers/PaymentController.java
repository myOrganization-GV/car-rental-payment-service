package com.gui.car_rental_payment_service.controllers;

import com.gui.car_rental_payment_service.dtos.PixPaymentDto;
import com.gui.car_rental_payment_service.entities.MyPayment;
import com.gui.car_rental_payment_service.enums.MyPaymentStatus;
import com.gui.car_rental_payment_service.services.PaymentService;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

    // NOTE: Intentionally unauthenticated — this is a TEST-ONLY mock of the Mercado Pago PIX webhook.
    // The URL is embedded in the QR code and opened from a phone browser, so it cannot carry a bearer
    // token. Guarded only by the random sagaId (UUID) + idempotency check. Known limitation, not for
    // production use. See IMPROVEMENTS.md (0.2) for the hardening option (single-use confirm token).
    @GetMapping("/{sagaId}/mock-confirm")
    public ResponseEntity<String> mockConfirmPixPayment(@PathVariable UUID sagaId) {
        Optional<MyPayment> optional = paymentService.getPaymentBySagaId(sagaId);

        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MyPayment payment = optional.get();

        if (payment.getMyPaymentStatus() == MyPaymentStatus.COMPLETED) {
            return ResponseEntity.ok("""
            <html><body style="font-family:sans-serif;text-align:center;padding:40px">
              <h2>✅ Payment already confirmed!</h2>
            </body></html>
        """);
        }

        paymentService.confirmMockPixPayment(payment);

        return ResponseEntity.ok("""
        <html><body style="font-family:sans-serif;text-align:center;padding:40px">
          <h2>✅ PIX Payment Confirmed!</h2>
          <p>Your rental payment has been successfully processed.</p>
          <p style="color:gray;font-size:12px">You can close this tab.</p>
        </body></html>
    """);
    }

}
