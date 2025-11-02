package com.vanmo.booking_app_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "room_types")
public class RoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String name;
    @Column(length = 5000, columnDefinition = "TEXT")
    String description;

    @OneToMany(mappedBy = "roomType", cascade = CascadeType.ALL)
    Set<Room> rooms;
}
