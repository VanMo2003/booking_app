package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
    boolean existsByUsername(String username);
}
