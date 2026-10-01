package com.vanmo.booking_app_be.entity;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * A sightseeing tour a branch offers its guests (city walk, day trip to Hạ Long…). Shown on the hotel page and known
 * to the chat assistant; booking a tour still goes through the hotel's staff. Written by the branch's owner or
 * manager only.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "tours")
public class Tour {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull
    String name;

    @Column(length = 5000, columnDefinition = "TEXT")
    String description;

    /** Per guest. */
    @NotNull
    @Column(nullable = false, precision = 12)
    BigDecimal price;

    /** Free text: "4 giờ", "1 ngày", "2 ngày 1 đêm". */
    @Column(length = 100)
    String duration;

    /** When it leaves: "8:00 hằng ngày", "Thứ Bảy và Chủ Nhật". */
    @Column(length = 255)
    String departure;

    /** What the price covers: transport, tickets, guide, meals… */
    @Column(length = 1000)
    String includes;

    /** Largest group; null = not limited. */
    Integer maxGuests;

    /** Uploaded file (`/uploads/...`) or an external image URL. */
    @Column(length = 1000)
    String pathImage;

    /** False while the tour is paused ("tạm ngừng"); it stays listed, greyed out. */
    @Builder.Default
    boolean available = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    @CreationTimestamp
    @Column(updatable = false)
    Date onCreate;

    @UpdateTimestamp
    Date onUpdate;
}
