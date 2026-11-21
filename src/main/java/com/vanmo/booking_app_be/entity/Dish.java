package com.vanmo.booking_app_be.entity;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.vanmo.booking_app_be.entity.enums.DishCategory;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** One item on a branch's food menu. Written by the branch's owner or manager only. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "dishes")
public class Dish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull
    String name;

    @Column(length = 5000, columnDefinition = "TEXT")
    String description;

    @NotNull
    @Column(nullable = false, precision = 12)
    BigDecimal price;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    DishCategory category;

    /** Uploaded file (`/uploads/...`) or an external image URL. */
    @Column(length = 1000)
    String pathImage;

    /** False while the kitchen can't serve it ("tạm hết"); the dish stays on the menu. */
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
