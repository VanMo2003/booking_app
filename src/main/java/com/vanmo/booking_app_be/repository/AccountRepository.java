package com.vanmo.booking_app_be.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.enums.Role;

public interface AccountRepository extends JpaRepository<Account, String> {
    boolean existsByUsername(String username);

    Optional<Account> findByUsername(String username);

    List<Account> findAllByRole(Role role);
}
