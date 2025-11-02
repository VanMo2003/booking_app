package com.vanmo.booking_app_be.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
    String name;
    String address;
    @Pattern(
            regexp = "0\\d{9}",
            message = "Phone number is not in correct format"
    )
    @NotNull
    String phone;
    @Column(length = 5000)
    String description;
    String category;
    double rating;

    @OneToOne
    @JoinColumn(name = "account_id")
    Account account;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<HotelImage> hotelImages;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<Room> rooms;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL)
    Set<Service> services;

    boolean active;
    
    @CreationTimestamp
    @Column(updatable = false)
    Date onCreate;

    @UpdateTimestamp
    Date onUpdate;
}
