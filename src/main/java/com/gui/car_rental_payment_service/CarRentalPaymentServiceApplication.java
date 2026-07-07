package com.gui.car_rental_payment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class CarRentalPaymentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CarRentalPaymentServiceApplication.class, args);
	}

}
