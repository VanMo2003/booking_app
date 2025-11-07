package com.vanmo.booking_app_be.entity;

import com.vanmo.booking_app_be.entity.enums.RoomStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "rooms", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hotel_id", "name"})
})
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(length = 20, columnDefinition = "TEXT")
    @NotNull
    String roomNumber;
    @NotNull(message = "Price cannot be left blank")
    @Column(nullable = false, precision = 12)
    BigDecimal price;
    @Column(length = 5000, columnDefinition = "TEXT")
    String description;
    Integer capacity;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<RoomImage> roomImages;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<RoomAmenity> roomAmenities;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_type_id")
    RoomType roomType;

    @Enumerated(EnumType.STRING)
    RoomStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    Date onCreate;

    @UpdateTimestamp
    Date onUpdate;
}
