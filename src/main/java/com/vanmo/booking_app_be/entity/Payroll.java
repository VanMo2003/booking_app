package com.vanmo.booking_app_be.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.vanmo.booking_app_be.entity.enums.PayrollStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "payrolls")
public class Payroll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    LocalDate month;

    @NotNull
    @Column(precision = 12)
    BigDecimal totalSalary;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    Employee employee;

    @NotNull
    @Enumerated(EnumType.STRING)
    PayrollStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    Date onCreate;

    @UpdateTimestamp
    Date onUpdate;
}
