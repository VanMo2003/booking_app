package com.vanmo.booking_app_be.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "reports")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "hotel_id")
    Hotel hotel;

    @NotNull
    LocalDate reportingMonth;

    @NotNull
    @Column(precision = 15)
    BigDecimal totalRevenue;

    @NotNull
    @Min(value = 0)
    Integer totalBooking;

    @NotNull
    @Min(value = 0)
    Integer totalService;

    @CreationTimestamp
    @Column(updatable = false)
    Date onCreate;

    @UpdateTimestamp
    Date onUpdate;
}
