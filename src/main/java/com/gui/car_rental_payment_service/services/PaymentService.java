package com.gui.car_rental_payment_service.services;

import com.gui.car_rental_common.events.payment.PaymentCreatedEvent;
import com.gui.car_rental_common.events.payment.PaymentCreationFailedEvent;
import com.gui.car_rental_payment_service.entities.Payment;
import com.gui.car_rental_payment_service.enums.PaymentMethod;
import com.gui.car_rental_payment_service.repositories.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.gui.car_rental_common.commands.PaymentCreationCommand;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@KafkaListener(topics = "rental-saga-payment-commands", groupId = "payment-service-group")
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    public PaymentService(PaymentRepository paymentRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.paymentRepository = paymentRepository;
        this.kafkaTemplate = kafkaTemplate;
    }


    public Optional<Payment> getPaymentById(UUID paymentId) {return paymentRepository.findById(paymentId);}


    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment savePayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    public void deletePaymentById(UUID paymentId) {
        paymentRepository.deleteById(paymentId);
    }

    @KafkaHandler
    public Payment consumePaymentCreationCommand(PaymentCreationCommand command){

        try {
            Payment payment = new Payment();
            payment.setPaymentMethod(PaymentMethod.valueOf(command.getBookingDto().getPaymentDto().getPaymentMethod()));
            Payment savedPayment= paymentRepository.save(payment);

            PaymentCreatedEvent paymentCreatedEvent = new PaymentCreatedEvent(
                    command.getSagaTransactionId(), command.getBookingDto());
            kafkaTemplate.send("payment-service-events", paymentCreatedEvent);
            logger.info("Published PaymentCreatedEvent for Saga ID: {}", command.getSagaTransactionId());

            return savedPayment;
        } catch (Exception e) {
            logger.error("Error processing PaymentCreationCommand for Saga ID {}: {}",
                    command.getSagaTransactionId(), e.getMessage());
            PaymentCreationFailedEvent failedEvent = new PaymentCreationFailedEvent(command.getSagaTransactionId(), command.getBookingDto());
            failedEvent.setMessage("Payment creation failed: " + e.getMessage());
            kafkaTemplate.send("payment-service-events", failedEvent);

            logger.info("Published PaymentCreationFailedEvent for Saga ID: {}", command.getSagaTransactionId());
            return null;
        }

    }


}
