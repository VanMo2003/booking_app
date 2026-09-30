package com.vanmo.booking_app_be.entity;

import java.util.Date;
import java.util.Set;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.Length;

import com.vanmo.booking_app_be.entity.enums.HotelStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

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

    @Pattern(regexp = "0\\d{9}", message = "Phone number is not in correct format")
    @Length(min = 10, max = 10, message = "Phone number must have exactly 10 digits")
    @NotNull
    String phone;

    @Column(length = 5000, columnDefinition = "TEXT")
    String description;

    @NotNull
    String category; // Luxury, Grand, Holiday, Standard

    double rating;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    Account account;

    @ManyToOne
    @JoinColumn(name = "hotel_chain_id", nullable = false)
    HotelChain hotelChain;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<HotelImage> hotelImages;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<Room> rooms;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL)
    Set<Service> services;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL)
    Set<Amenity> amenities;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<Dish> dishes;

    boolean active;

    HotelStatus status;

    /** Chat assistant answers customers when nobody on the team has replied in time. Null (old rows) = off. */
    @Column(name = "ai_auto_reply")
    Boolean aiAutoReply;

    @CreationTimestamp
    @Column(updatable = false)
    Date onCreate;

    @UpdateTimestamp
    Date onUpdate;
}
