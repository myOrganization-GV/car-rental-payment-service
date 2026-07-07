package com.gui.car_rental_payment_service.services;

import com.gui.car_rental_common.dtos.PaymentDto;
import com.gui.car_rental_common.events.payment.PaymentCreatedEvent;
import com.gui.car_rental_common.events.payment.PaymentCreationFailedEvent;
import com.gui.car_rental_payment_service.dtos.MockPixResult;
import com.gui.car_rental_payment_service.dtos.PixPaymentDto;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gui.car_rental_common.commands.PaymentCreationCommand;
import com.gui.car_rental_payment_service.entities.ProcessedMessage;
import com.gui.car_rental_payment_service.repositories.ProcessedMessageRepository;

import java.math.BigDecimal;
import java.util.*;

@Service
@KafkaListener(topics = "rental-saga-payment-commands", groupId = "payment-service-group")
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxService outboxService;
    private final ProcessedMessageRepository processedMessageRepository;
    @Value("${app.base-url}")
    private String baseUrl;
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private static final String PAYMENT_EVENTS_TOPIC = "payment-service-events";
    private static final String PAYMENT_CREATION = "PAYMENT_CREATION";

    public PaymentService(PaymentRepository paymentRepository,
                          OutboxService outboxService,
                          ProcessedMessageRepository processedMessageRepository) {
        this.paymentRepository = paymentRepository;
        this.outboxService = outboxService;
        this.processedMessageRepository = processedMessageRepository;
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

    public PixPaymentDto getPixPaymentDataDto( UUID sagaId) throws MPException, MPApiException {

        Optional<MyPayment> myPaymentOptional = paymentRepository.findBySagaId(sagaId);
        if(myPaymentOptional.isEmpty()){
            return null;
        }

        MyPayment myPayment = myPaymentOptional.get();
        if (myPayment.getMockQrCode() != null) {
            PixPaymentDto dto = new PixPaymentDto();
            dto.setQrCode(myPayment.getMockQrCode());
            dto.setQrCodeBase64(myPayment.getMockQrCodeBase64());
            dto.setSagaId(sagaId);
            return dto;
        }
        try {
            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.get(Long.parseLong(myPayment.getMercadoPagoId()));

            PixPaymentDto pixPaymentDto = new PixPaymentDto();

            String qrCode = payment.getPointOfInteraction().getTransactionData().getQrCode();
            String qrCodeBase64 = payment.getPointOfInteraction().getTransactionData().getQrCodeBase64();

            pixPaymentDto.setQrCode(qrCode);
            pixPaymentDto.setQrCodeBase64(qrCodeBase64);
            pixPaymentDto.setSagaId(sagaId);
            return pixPaymentDto;
        } catch (MPApiException e) {
            System.err.println("Mercado Pago API Exception: " + e.getApiResponse().getContent());
            throw e;
        } catch (MPException e) {
            System.err.println("Mercado Pago Exception: " + e.getMessage());
            throw e;
        }
    }
    public void confirmMockPixPayment(MyPayment payment) {
        payment.setMyPaymentStatus(MyPaymentStatus.COMPLETED);
        paymentRepository.save(payment);

        logger.info("Mock PIX confirmed and event published for sagaId: {}", payment.getSagaId());
    }
    @Transactional
    @KafkaHandler
    public MyPayment consumePaymentCreationCommand(PaymentCreationCommand command){

        UUID sagaId = command.getSagaTransactionId();
        if (!claim(sagaId, PAYMENT_CREATION)) {
            logger.info("Duplicate PaymentCreationCommand for saga {} — skipping", sagaId);
            return null;
        }

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
                    MockPixResult mockPix = MockPixService.generate(
                            command.getSagaTransactionId(), baseUrl
                    );

                    myPayment.setBookingId(command.getBookingDto().getBookingId());
                    myPayment.setCarId(command.getBookingDto().getCarId());
                    myPayment.setAmount(command.getBookingDto().getAmount());
                    myPayment.setMercadoPagoId("MOCK-PIX-" + command.getSagaTransactionId());
                    myPayment.setMyPaymentStatus(MyPaymentStatus.PENDING);
                    myPayment.setSagaId(command.getSagaTransactionId());
                    myPayment.setMockQrCode(mockPix.getQrCode());
                    myPayment.setMockQrCodeBase64(mockPix.getQrCodeBase64());
                    savedMyPayment = paymentRepository.save(myPayment);
                    break;
                case "CREDIT_CARD":
                    mercadoPagoPayment = createCardPayment(command.getBookingDto().getPaymentDto().getCardToken(),
                                            command.getBookingDto().getAmount(), "car rental card payment", command.getBookingDto().getBookingId().toString(),
                            paymentDto.getPayerEmail(), paymentDto.getPayerFirstName(), paymentDto.getPayerLastName(),
                            paymentDto.getPayerIdentificationType(), paymentDto.getPayerIdentificationNumber());

                    myPayment.setBookingId(command.getBookingDto().getBookingId());
                    myPayment.setCarId(command.getBookingDto().getCarId());
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
            outboxService.saveEvent(PAYMENT_EVENTS_TOPIC, paymentCreatedEvent);
            logger.info("Queued PaymentCreatedEvent to outbox for Saga ID: {}", command.getSagaTransactionId());

            return savedMyPayment;
        } catch (Exception e) {
            logger.error("Error processing PaymentCreationCommand for Saga ID {}: {}",
                    command.getSagaTransactionId(), e.getMessage());
            PaymentCreationFailedEvent failedEvent = new PaymentCreationFailedEvent(command.getSagaTransactionId(), command.getBookingDto());
            failedEvent.setMessage("Payment creation failed: " + e.getMessage());
            outboxService.saveEvent(PAYMENT_EVENTS_TOPIC, failedEvent);

            logger.info("Queued PaymentCreationFailedEvent to outbox for Saga ID: {}", command.getSagaTransactionId());
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
                                IdentificationRequest.builder()// Use SDK IdentificationRequest meaning documents numbers and types.
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
                        .externalReference(externalReference) // my internal order ID to match my own database
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


    public Optional<MyPayment> getPaymentBySagaId(UUID sagaId) {
        return paymentRepository.findBySagaId(sagaId);
    }

    private boolean claim(UUID sagaId, String messageType) {
        if (processedMessageRepository.existsBySagaIdAndMessageType(sagaId, messageType)) {
            return false;
        }
        processedMessageRepository.save(new ProcessedMessage(sagaId, messageType));
        return true;
    }
}
