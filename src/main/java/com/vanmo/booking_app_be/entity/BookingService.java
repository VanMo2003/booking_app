package com.vanmo.booking_app_be.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "booking_services")
public class BookingService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    Booking booking;

    @ManyToOne
    @JoinColumn(name = "service_id")
    Service service;

    @Min(value = 1)
    Integer quantity;

    @NotNull(message = "Total price cannot be left blank")
    @Column(nullable = false, precision = 12)
    BigDecimal total;
}
