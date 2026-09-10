package com.vanmo.booking_app_be.entity;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "booking_rooms")
public class BookingRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    Booking booking;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    Room room;
}
