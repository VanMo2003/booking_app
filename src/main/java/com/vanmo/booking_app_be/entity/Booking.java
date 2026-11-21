package com.vanmo.booking_app_be.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.vanmo.booking_app_be.entity.enums.BookingStatus;
import com.vanmo.booking_app_be.entity.enums.ChatSide;
import com.vanmo.booking_app_be.entity.enums.PaymentMethod;
import com.vanmo.booking_app_be.entity.enums.PaymentStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull
    LocalDate checkinDate;

    @NotNull
    LocalDate checkoutDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    BookingStatus bookingStatus;

    @NotNull
    @Enumerated(EnumType.STRING)
    PaymentMethod paymentMethod;

    @NotNull
    @Enumerated(EnumType.STRING)
    PaymentStatus paymentStatus;

    @Column
    LocalDateTime paymentExpireAt;

    @Column
    LocalDateTime paidAt;

    @ManyToOne
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    Customer customer;

    /**
     * Staff/manager account that took this booking at the front desk; null when the customer
     * booked it themselves. Drives the staff-performance report.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by_account_id")
    Account handledBy;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<BookingRoom> bookingRooms;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    Set<BookingService> bookingServices;

    @NotNull(message = "Total amount cannot be left blank")
    @Column(nullable = false, precision = 12)
    BigDecimal totalAmount;

    @Column(length = 5000, columnDefinition = "TEXT")
    String note;

    /** When the booking was cancelled and from which side; null for bookings cancelled before this was recorded. */
    Date canceledAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    ChatSide canceledBy;

    @CreationTimestamp
    @Column(updatable = false)
    Date onCreate;

    @UpdateTimestamp
    Date onUpdate;
}
