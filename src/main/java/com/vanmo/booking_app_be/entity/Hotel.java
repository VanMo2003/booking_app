package com.vanmo.booking_app_be.entity;

import com.vanmo.booking_app_be.entity.enums.HotelStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.Length;

import java.util.Date;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "hotels")
public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String pathImage;
    @NotNull
    String name;
    @NotNull
    String address;
    @Pattern(
            regexp = "0\\d{9}",
            message = "Phone number is not in correct format"
    )
    @Length(min = 10, max = 10, message = "Phone number must have exactly 10 digits")
    @NotNull
    String phone;
    @Column(length = 5000, columnDefinition = "TEXT")
    String description;
    @NotNull
    String category; // Luxury, Grand, Holiday, Standard
    double rating;

    @OneToOne
    @JoinColumn(name = "account_id", nullable = false)
    Account account;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<HotelImage> hotelImages;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<Room> rooms;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL)
    Set<Service> services;

    @OneToMany(mappedBy = "hotel",cascade = CascadeType.ALL)
    Set<Amenity> amenities;
    boolean active;

    HotelStatus status;
    
    @CreationTimestamp
    @Column(updatable = false)
    Date onCreate;

    @UpdateTimestamp
    Date onUpdate;
}
