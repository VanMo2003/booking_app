package com.vanmo.booking_app_be.entity;

import java.util.Date;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.Length;

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

    @OneToOne
    @JoinColumn(name = "account_id", nullable = false)
    Account account;

    String fullName;

    @Pattern(regexp = "0\\d{9}", message = "Phone number is not in correct format")
    @Length(min = 10, max = 10, message = "Phone number must have exactly 10 digits")
    @NotNull
    String phoneNumber;

    String gender;

    String hometown;

    @CreationTimestamp
    @Column(updatable = false)
    Date onCreate;

    @UpdateTimestamp
    Date onUpdate;
}
