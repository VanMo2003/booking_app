package com.vanmo.booking_app_be.service.impl;

import java.util.List;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.vanmo.booking_app_be.dto.request.CustomerCreateRequest;
import com.vanmo.booking_app_be.dto.request.CustomerUpdateRequest;
import com.vanmo.booking_app_be.dto.response.CustomerResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Customer;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.CustomerMapper;
import com.vanmo.booking_app_be.repository.AccountRepository;
import com.vanmo.booking_app_be.repository.BookingRepository;
import com.vanmo.booking_app_be.repository.CustomerRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.CustomerService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final BookingRepository bookingRepository;
    private final CustomerMapper customerMapper;
    private final AccessGuard accessGuard;

    @Override
    public CustomerResponse create(CustomerCreateRequest req) {
        Customer entity = customerMapper.toEntity(req);

        if (req.getAccountId() == null || req.getAccountId().isBlank()) {
            // walk-in guest: front-desk staff records name/phone, no login account yet
            accessGuard.requireStaffOrAbove();
        } else {
            accessGuard.requireSelfOrAdmin(req.getAccountId());
            Account acc = accountRepository
                    .findById(req.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found id=" + req.getAccountId()));
            entity.setAccount(acc);
        }

        return customerMapper.toResponse(customerRepository.save(entity));
    }

    @Override
    public CustomerResponse update(Long id, CustomerUpdateRequest req) {
        Customer entity = customerRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found id=" + id));
        customerMapper.updateEntity(entity, req);
        return customerMapper.toResponse(customerRepository.save(entity));
    }

    @Override
    public CustomerResponse getById(Long id) {
        Customer entity = customerRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found id=" + id));
        return customerMapper.toResponse(entity);
    }

    @Override
    public PageResponse<CustomerResponse> getAll(int page, int size) {
        accessGuard.requireAdmin();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<CustomerResponse> data = customerRepository.findAll(pageable).map(customerMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public List<CustomerResponse> getAllByHotel(Long hotelId) {
        accessGuard.requireBranchStaffOrAbove(hotelId);
        return bookingRepository.findDistinctCustomersByHotelId(hotelId).stream()
                .map(customerMapper::toResponse)
                .toList();
    }

    @Override
    public List<CustomerResponse> searchByPhone(String phoneNumber) {
        accessGuard.requireStaffOrAbove();
        return customerRepository.findAllByPhoneNumber(phoneNumber).stream()
                .map(customerMapper::toResponse)
                .toList();
    }

    @Override
    public CustomerResponse linkByPhone(String phoneNumber) {
        Account caller = accessGuard.currentAccount();
        if (caller.getRole() != Role.CUSTOMER) {
            throw new AppException(ErrorCode.UN_AUTHORIZED, "Only a customer account can claim a walk-in profile");
        }
        if (customerRepository.findByAccount_Id(caller.getId()) != null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "This account already has a customer profile");
        }

        Customer walkIn = customerRepository.findAllByPhoneNumberAndAccountIsNull(phoneNumber).stream()
                .findFirst()
                .orElseThrow(
                        () -> new ResourceNotFoundException("No unlinked walk-in profile with phone=" + phoneNumber));

        walkIn.setAccount(caller);
        return customerMapper.toResponse(customerRepository.save(walkIn));
    }

    @Override
    public void delete(Long id) {
        Customer entity = customerRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found id=" + id));
        customerRepository.delete(entity);
    }
}
