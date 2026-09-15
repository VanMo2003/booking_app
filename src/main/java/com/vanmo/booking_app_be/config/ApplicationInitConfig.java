package com.vanmo.booking_app_be.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.repository.AccountRepository;
import com.vanmo.booking_app_be.repository.HotelChainRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;

    @NonFinal
    static String ADMIN_USER_NAME = "admin123";

    @NonFinal
    static String ADMIN_PASSWORD = "admin123";

    /**
     * Be run every time the application start
     * @return
     */
    @Bean
    ApplicationRunner applicationRunner(
            AccountRepository accountRepository, HotelChainRepository hotelChainRepository) {
        return args -> {
            if (accountRepository.findByUsername(ADMIN_USER_NAME).isEmpty()) {

                Account account = Account.builder()
                        .username(ADMIN_USER_NAME)
                        .password(passwordEncoder.encode(ADMIN_PASSWORD))
                        .role(Role.ADMIN)
                        .status(true)
                        .build();

                accountRepository.save(account);
                log.warn(
                        "{ADMIN_USER_NAME} user has been create with default password: {ADMIN_PASSWORD}, please change it");
            }

            // Chains opened before owner approval existed were already live - keep them running.
            int approved = hotelChainRepository.approveLegacyChains();
            if (approved > 0) {
                log.info("Marked {} existing hotel chain(s) as APPROVED", approved);
            }
        };
    }
}
