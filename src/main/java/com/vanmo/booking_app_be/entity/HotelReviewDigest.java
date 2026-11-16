package com.vanmo.booking_app_be.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * What guests keep saying about a branch, written once by the model and kept until the reviews change. One row per
 * branch; it is a cache, so deleting it only costs one model call.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "hotel_review_digests",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"hotel_id"})})
public class HotelReviewDigest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne
    @JoinColumn(name = "hotel_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Hotel hotel;

    /** Two or three sentences a guest reads before booking. */
    @Column(length = 2000, columnDefinition = "TEXT")
    String summary;

    /** What guests praise, one per line. */
    @Column(length = 2000, columnDefinition = "TEXT")
    String pros;

    /** What they complain about, one per line - left empty when nobody complained. */
    @Column(length = 2000, columnDefinition = "TEXT")
    String cons;

    /** How many reviews it was written from, and their average - the digest is redone when the count changes. */
    int reviewCount;

    double averageRating;

    Instant generatedAt;
}
