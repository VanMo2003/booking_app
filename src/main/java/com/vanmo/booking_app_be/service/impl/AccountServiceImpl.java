package com.vanmo.booking_app_be.service.impl;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.vanmo.booking_app_be.dto.request.AccountCreateRequest;
import com.vanmo.booking_app_be.dto.request.AccountUpdateRequest;
import com.vanmo.booking_app_be.dto.response.AccountResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.AccountMapper;
import com.vanmo.booking_app_be.repository.AccountRepository;
import com.vanmo.booking_app_be.repository.DeviceTokenRepository;
import com.vanmo.booking_app_be.repository.NotificationRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.AccountService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final AccessGuard accessGuard;

    @Override
    public AccountResponse create(AccountCreateRequest req) {
        Role requestedRole = Role.valueOf(req.getRole());
        // Owners sign themselves up (POST /owner-registrations) and wait for review; admins never mint accounts.
        switch (requestedRole) {
            case CUSTOMER -> {
                // self-signup, no authenticated caller required
            }
            case STAFF -> throw new AppException(
                    ErrorCode.BAD_REQUEST, "Staff accounts must be created via POST /employees");
            case HOTEL_OWNER -> throw new AppException(
                    ErrorCode.BAD_REQUEST, "Hotel owner accounts must register via POST /owner-registrations");
            case ADMIN -> throw new AppException(ErrorCode.UN_AUTHORIZED, "Admin accounts cannot be created");
            case HOTEL_MANAGER -> accessGuard.requireApprovedOwner();
        }

        Account entity = createAccount(req);
        return accountMapper.toResponse(entity);
    }

    Account createAccount(AccountCreateRequest req) {
        boolean checkUsername = accountRepository.existsByUsername(req.getUsername());
        if (checkUsername) {
            throw new ResourceNotFoundException("Account existed with username: " + req.getUsername());
        }
        Account entity = accountMapper.toEntity(req);
        entity.setPassword(passwordEncoder.encode(req.getPassword()));
        return accountRepository.save(entity);
    }

    @Override
    public AccountResponse update(String id, AccountUpdateRequest req) {
        Account current = accessGuard.currentAccount();
        boolean isAdmin = current.getRole() == Role.ADMIN;
        boolean isSelf = current.getId().equals(id);

        if (req.getRole() != null) {
            throw new AppException(ErrorCode.UN_AUTHORIZED, "Account role cannot be changed");
        }
        if (!isAdmin && !isSelf) {
            throw new AppException(ErrorCode.UN_AUTHORIZED);
        }
        // An admin's review lever is locking/unlocking; only the account holder changes its own password.
        if (!isSelf && req.getPassword() != null) {
            throw new AppException(ErrorCode.UN_AUTHORIZED, "Only the account holder can change its password");
        }
        if (!isAdmin && req.getStatus() != null) {
            throw new AppException(ErrorCode.UN_AUTHORIZED, "Only ADMIN can lock or unlock accounts");
        }

        Account entity = accountRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found id=" + id));
        accountMapper.updateEntity(entity, req);
        return accountMapper.toResponse(accountRepository.save(entity));
    }

    @Override
    public AccountResponse getById(String id) {
        Account entity = accountRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found id=" + id));
        return accountMapper.toResponse(entity);
    }

    @Override
    public PageResponse<AccountResponse> getAll(int page, int size) {
        accessGuard.requireAdmin();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<AccountResponse> data = accountRepository.findAll(pageable).map(accountMapper::toResponse);
        return PageResponse.from(data);
    }

    @Transactional
    @Override
    public void delete(String id) {
        accessGuard.requireAdmin();
        Account entity = accountRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found id=" + id));
        // The inbox and device registrations belong to the account and go with it.
        notificationRepository.deleteAllByAccountId(id);
        deviceTokenRepository.deleteAllByAccountId(id);
        accountRepository.delete(entity);
    }
}
