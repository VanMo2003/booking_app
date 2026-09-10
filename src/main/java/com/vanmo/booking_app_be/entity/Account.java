package com.vanmo.booking_app_be.entity;

import java.util.Date;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.Length;

import com.vanmo.booking_app_be.entity.enums.Role;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @NotNull
    @Column(unique = true)
    @Length(min = 6, message = "Username must be at least 6 characters")
    String username;

    @NotNull
    @Length(min = 6, message = "Password must be at least 6 characters")
    String password;

    @NotNull
    @Enumerated(EnumType.STRING)
    Role role;

    boolean status;

    @CreationTimestamp
    @Column(updatable = false)
    Date onCreate;

    @UpdateTimestamp
    Date onUpdate;
}
