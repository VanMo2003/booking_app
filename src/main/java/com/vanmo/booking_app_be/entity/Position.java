package com.vanmo.booking_app_be.entity;

import java.util.Set;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "positions")
public class Position {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull
    String name;

    @Column(length = 5000, columnDefinition = "TEXT")
    String description;

    @OneToMany(mappedBy = "position", cascade = CascadeType.ALL)
    Set<Employee> employees;
}
