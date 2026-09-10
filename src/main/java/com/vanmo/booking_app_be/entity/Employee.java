package com.vanmo.booking_app_be.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.Set;

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
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String pathImage;

    @OneToOne
    @JoinColumn(name = "account_id", nullable = false)
    Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    @NotNull
    String fullName;

    @Pattern(regexp = "0\\d{9}", message = "Phone number is not in correct format")
    @Length(min = 10, max = 10, message = "Phone number must have exactly 10 digits")
    @NotNull
    String phoneNumber;

    @NotNull
    String gender;

    @NotNull
    LocalDate dateOfBirth;

    @NotNull
    String hometown;

    @NotNull
    @Column(nullable = false, precision = 12)
    BigDecimal salary;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<Payroll> payrolls;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "position_id", nullable = false)
    Position position;

    @CreationTimestamp
    @Column(updatable = false)
    Date onCreate;

    @UpdateTimestamp
    Date onUpdate;
}
