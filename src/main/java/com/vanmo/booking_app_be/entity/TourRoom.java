package com.vanmo.booking_app_be.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * A room that comes with a tour package: the hotel picks which rooms (cheap or expensive) a tour includes, and the
 * guest chooses among the ones still free for the stay. Removed with either the tour or the room.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "tour_rooms", uniqueConstraints = @UniqueConstraint(columnNames = {"tour_id", "room_id"}))
public class TourRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Tour tour;

    // Eager: the room's number, type and price are what every reader of a link wants.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Room room;
}
