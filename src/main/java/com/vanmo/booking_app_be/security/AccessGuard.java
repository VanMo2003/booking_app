package com.vanmo.booking_app_be.security;

import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Employee;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.HotelChain;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.repository.AccountRepository;
import com.vanmo.booking_app_be.repository.EmployeeRepository;
import com.vanmo.booking_app_be.repository.HotelChainRepository;
import com.vanmo.booking_app_be.repository.HotelRepository;

import lombok.RequiredArgsConstructor;

/**
 * Central place for "is the current account allowed to touch this chain/branch" checks.
 * Ownership checks need DB joins (chain -> owner, branch -> manager, staff -> their one branch),
 * so they live here as reusable service-layer calls rather than as SpEL on each controller method.
 */
@Component
@RequiredArgsConstructor
public class AccessGuard {

    private final AccountRepository accountRepository;
    private final HotelRepository hotelRepository;
    private final HotelChainRepository hotelChainRepository;
    private final EmployeeRepository employeeRepository;

    public Account currentAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UN_AUTHENTICATED);
        }
        return accountRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.UN_AUTHENTICATED));
    }

    public void requireAdmin() {
        if (currentAccount().getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.UN_AUTHORIZED);
        }
    }

    public void requireSelfOrAdmin(String accountId) {
        Account current = currentAccount();
        if (current.getRole() != Role.ADMIN && !current.getId().equals(accountId)) {
            throw new AppException(ErrorCode.UN_AUTHORIZED);
        }
    }

    public HotelChain requireChainOwner(Long hotelChainId) {
        Account current = currentAccount();
        HotelChain chain = hotelChainRepository
                .findById(hotelChainId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel chain not found with id=" + hotelChainId));

        if (current.getRole() == Role.ADMIN) {
            return chain;
        }
        if (current.getRole() == Role.HOTEL_OWNER && chain.getAccount().getId().equals(current.getId())) {
            return chain;
        }
        throw new AppException(ErrorCode.UN_AUTHORIZED);
    }

    public Hotel requireBranchManagerOrAbove(Long hotelId) {
        Account current = currentAccount();
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id=" + hotelId));

        boolean allowed =
                switch (current.getRole()) {
                    case ADMIN -> true;
                    case HOTEL_OWNER -> hotel.getHotelChain()
                            .getAccount()
                            .getId()
                            .equals(current.getId());
                    case HOTEL_MANAGER -> hotel.getAccount().getId().equals(current.getId());
                    default -> false;
                };

        if (!allowed) {
            throw new AppException(ErrorCode.UN_AUTHORIZED);
        }
        return hotel;
    }

    public Hotel requireBranchStaffOrAbove(Long hotelId) {
        Account current = currentAccount();
        if (current.getRole() != Role.STAFF) {
            return requireBranchManagerOrAbove(hotelId);
        }

        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id=" + hotelId));

        Employee employee = employeeRepository.findByAccount_Id(current.getId());
        if (employee == null || !employee.getHotel().getId().equals(hotelId)) {
            throw new AppException(ErrorCode.UN_AUTHORIZED);
        }
        return hotel;
    }

    public List<Long> hotelIdsForChain(Long hotelChainId) {
        return hotelRepository.findAllByHotelChain_Id(hotelChainId).stream()
                .map(Hotel::getId)
                .toList();
    }
}
