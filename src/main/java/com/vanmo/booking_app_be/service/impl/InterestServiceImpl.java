package com.vanmo.booking_app_be.service.impl;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Customer;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.entity.enums.TravelTheme;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.repository.CustomerRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.InterestService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InterestServiceImpl implements InterestService {
    private final CustomerRepository customerRepository;
    private final AccessGuard accessGuard;

    @Override
    @Transactional(readOnly = true)
    public Set<TravelTheme> mine() {
        return copy(customer().getInterests());
    }

    @Override
    @Transactional
    public Set<TravelTheme> replace(Set<TravelTheme> interests) {
        Customer customer = customer();
        customer.getInterests().clear();
        customer.getInterests().addAll(interests);
        return copy(customerRepository.save(customer).getInterests());
    }

    private Customer customer() {
        Account caller = accessGuard.currentAccount();
        if (caller.getRole() != Role.CUSTOMER) {
            throw new AppException(ErrorCode.UN_AUTHORIZED);
        }
        Customer customer = customerRepository.findByAccount_Id(caller.getId());
        if (customer == null) {
            throw new AppException(ErrorCode.CUSTOMER_PROFILE_REQUIRED);
        }
        return customer;
    }

    private static Set<TravelTheme> copy(Set<TravelTheme> interests) {
        return interests.isEmpty() ? EnumSet.noneOf(TravelTheme.class) : EnumSet.copyOf(interests);
    }
}
