package com.gui.car_rental_payment_service.services;

import com.gui.car_rental_common.dtos.PaymentDto;
import com.gui.car_rental_common.events.payment.PaymentCreatedEvent;
import com.gui.car_rental_common.events.payment.PaymentCreationFailedEvent;
import com.gui.car_rental_payment_service.entities.MyPayment;
import com.gui.car_rental_payment_service.enums.MyPaymentMethod;
import com.gui.car_rental_payment_service.enums.MyPaymentStatus;
import com.gui.car_rental_payment_service.repositories.PaymentRepository;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.gui.car_rental_common.commands.PaymentCreationCommand;

import java.math.BigDecimal;
import java.util.*;

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


    public Optional<MyPayment> getPaymentById(UUID paymentId) {return paymentRepository.findById(paymentId);}


    public List<MyPayment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public MyPayment savePayment(MyPayment myPayment) {
        return paymentRepository.save(myPayment);
    }

    public void deletePaymentById(UUID paymentId) {
        paymentRepository.deleteById(paymentId);
    }

    @KafkaHandler
    public MyPayment consumePaymentCreationCommand(PaymentCreationCommand command){

        try {
            MyPayment myPayment = new MyPayment();
            String paymentMethod = command.getBookingDto().getPaymentDto().getPaymentMethod();
            myPayment.setMyPaymentMethod(MyPaymentMethod.valueOf(command.getBookingDto().getPaymentDto().getPaymentMethod()));
            System.out.println("Payment Method: " + command.getBookingDto().getPaymentDto().getPaymentMethod());
            Payment mercadoPagoPayment;
            MyPayment savedMyPayment;
            PaymentDto paymentDto = command.getBookingDto().getPaymentDto();
            switch (paymentMethod){
                case "PIX":

                    mercadoPagoPayment = createPixPayment(command.getBookingDto().getAmount()
                        ,"pix payment for car rental", paymentDto.getPayerEmail(),
                            paymentDto.getPayerFirstName(), paymentDto.getPayerLastName(),
                            paymentDto.getPayerIdentificationType(), paymentDto.getPayerIdentificationNumber(), command.getSagaTransactionId().toString()

                    );
                    myPayment.setBookingId(command.getBookingDto().getBookingId());
                    myPayment.setCarId(command.getBookingDto().getCarId());
                    myPayment.setUserId(command.getBookingDto().getUserId());
                    myPayment.setAmount(mercadoPagoPayment.getTransactionAmount());
                    myPayment.setMercadoPagoId(mercadoPagoPayment.getId().toString());
                    myPayment.setMyPaymentStatus(MyPaymentStatus.PENDING);
                    savedMyPayment = paymentRepository.save(myPayment);
                    break;
                case "CREDIT_CARD":
                    mercadoPagoPayment = createCardPayment(command.getBookingDto().getPaymentDto().getCardToken(),
                                            command.getBookingDto().getAmount(), "car rental card payment", "payment-id-1",
                            paymentDto.getPayerEmail(), paymentDto.getPayerFirstName(), paymentDto.getPayerLastName(),
                            paymentDto.getPayerIdentificationType(), paymentDto.getPayerIdentificationNumber());

                    myPayment.setBookingId(command.getBookingDto().getBookingId());
                    myPayment.setCarId(command.getBookingDto().getCarId());
                    myPayment.setUserId(command.getBookingDto().getUserId());
                    myPayment.setAmount(mercadoPagoPayment.getTransactionAmount());
                    myPayment.setMercadoPagoId(mercadoPagoPayment.getId().toString());
                    myPayment.setMyPaymentStatus(MyPaymentStatus.PENDING);
                    savedMyPayment = paymentRepository.save(myPayment);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported payment method: " + paymentMethod);
            }
            PaymentCreatedEvent paymentCreatedEvent = new PaymentCreatedEvent(
                    command.getSagaTransactionId(), command.getBookingDto());
            kafkaTemplate.send("payment-service-events", paymentCreatedEvent);
            logger.info("Published PaymentCreatedEvent for Saga ID: {}", command.getSagaTransactionId());

            return savedMyPayment;
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


    public Payment createPixPayment(BigDecimal amount, String description, String payerEmail,
                                    String payerFirstName, String payerLastName, String payerIdentificationType,
                                    String payerIdentificationNumber, String externalReference)  throws MPException, MPApiException  {

        IdentificationRequest identificationRequest = IdentificationRequest.builder()
                .type(payerIdentificationType)
                .number(payerIdentificationNumber)
                .build();

        PaymentPayerRequest payerRequest = PaymentPayerRequest.builder()
                .email(payerEmail)
                .firstName(payerFirstName)
                .lastName(payerLastName)
                .identification(identificationRequest)
                .build();


        PaymentCreateRequest paymentCreateRequest =
                PaymentCreateRequest.builder()
                        .transactionAmount(amount)
                        .description(description)
                        .paymentMethodId("pix")
                        .payer(payerRequest)
                        .externalReference(externalReference)
                        .installments(1)
                        .build();

        try {
            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.create(paymentCreateRequest);
            System.out.println("Pix Payment Created: " + payment.getId());
            System.out.println("Status: " + payment.getStatus());
            return payment;
        } catch (MPApiException e) {
            System.err.println("Mercado Pago API Exception: " + e.getApiResponse().getContent());
            throw e;
        } catch (MPException e) {
            System.err.println("Mercado Pago Exception: " + e.getMessage());
            throw e;
        }
    }



    public Payment createCardPayment(
            String token, BigDecimal transactionAmount, String description, String externalReference,
            String email, String firstName,String lastName, String identificationType,
            String identificationNumber
    ) throws MPException, MPApiException{

        String idempotencyKey = UUID.randomUUID().toString();
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("x-idempotency-key", idempotencyKey);

        MPRequestOptions requestOptions = MPRequestOptions.builder()
                .customHeaders(customHeaders)
                .build();

        PaymentPayerRequest payerRequest =
                PaymentPayerRequest.builder()
                        .email(email)
                        .firstName(firstName)
                        .lastName(lastName)
                        .identification(
                                IdentificationRequest.builder()// Use SDK IdentificationRequest
                                        .type(identificationType)
                                        .number(identificationNumber)
                                        .build())
                        .build();

        PaymentCreateRequest paymentCreateRequest =
                PaymentCreateRequest.builder()
                        .token(token)// Use the CardToken from the frontend
                        .transactionAmount(transactionAmount)
                        .description(description)
                        .installments(1)
                        .externalReference(externalReference) // my internal order ID
                        .payer(payerRequest)
                        .build();

        try {

            PaymentClient paymentClient = new PaymentClient();
            logger.info("Sending create card payment request with idempotency key: " + idempotencyKey);
            Payment payment = paymentClient.create(paymentCreateRequest, requestOptions);
            logger.info("Card Payment Created: " + payment.getId());
            logger.info("Status: " + payment.getStatus());

            return payment;
        } catch (MPApiException e) {
            System.err.println("Mercado Pago API Exception creating card payment: " + e.getApiResponse().getContent());
            throw e;
        } catch (MPException e) {
            System.err.println("Mercado Pago Exception creating card payment: " + e.getMessage());
            throw e;
        }
    }

}
