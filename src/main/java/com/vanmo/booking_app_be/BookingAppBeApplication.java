package com.vanmo.booking_app_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class BookingAppBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingAppBeApplication.class, args);
    }
}
