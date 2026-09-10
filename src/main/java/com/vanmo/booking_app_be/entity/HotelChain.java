package com.vanmo.booking_app_be.entity;

import java.util.Date;
import java.util.Set;

import jakarta.persistence.*;
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
@Table(name = "hotel_chains")
public class HotelChain {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull
    String name;

    @Column(length = 5000, columnDefinition = "TEXT")
    String description;

    String pathImage;

    @OneToOne
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    Account account;

    @OneToMany(mappedBy = "hotelChain")
    Set<Hotel> hotels;

    @CreationTimestamp
    @Column(updatable = false)
    Date onCreate;

    @UpdateTimestamp
    Date onUpdate;
}
