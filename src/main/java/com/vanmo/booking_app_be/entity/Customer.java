package com.vanmo.booking_app_be.entity;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.Length;

import com.vanmo.booking_app_be.entity.enums.TravelTheme;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String pathImage;

    /** Null for a walk-in guest created by staff at the front desk (no login account yet). */
    @OneToOne
    @JoinColumn(name = "account_id")
    Account account;

    String fullName;

    @Pattern(regexp = "0\\d{9}", message = "Phone number is not in correct format")
    @Length(min = 10, max = 10, message = "Phone number must have exactly 10 digits")
    @NotNull
    String phoneNumber;

    String gender;

    String hometown;

    /** What the guest enjoys, picked on the app's first screen: drives suggestions and promotion pushes. */
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "customer_interests", joinColumns = @JoinColumn(name = "customer_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "theme", length = 20)
    Set<TravelTheme> interests = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false)
    Date onCreate;

    @UpdateTimestamp
    Date onUpdate;
}
