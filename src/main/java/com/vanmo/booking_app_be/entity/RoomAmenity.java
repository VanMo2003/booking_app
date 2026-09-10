package com.vanmo.booking_app_be.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "room_amenity")
public class RoomAmenity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @JoinColumn(name = "room_id", nullable = false)
    @ManyToOne(fetch = FetchType.EAGER)
    Room room;

    @JoinColumn(name = "amenity_id", nullable = false)
    @ManyToOne(fetch = FetchType.EAGER)
    Amenity amenity;

    @NotNull
    @Min(value = 0)
    Integer quantity;
}
