package com.vanmo.booking_app_be.service.impl;

import com.vanmo.booking_app_be.dto.request.AccountCreateRequest;
import com.vanmo.booking_app_be.dto.request.AccountUpdateRequest;
import com.vanmo.booking_app_be.dto.response.AccountResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.AccountMapper;
import com.vanmo.booking_app_be.repository.AccountRepository;
import com.vanmo.booking_app_be.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AccountResponse create(AccountCreateRequest req) {
        Account entity = createAccount(req);
        AccountResponse accountResponse = accountMapper.toResponse(entity);
        return accountResponse;
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
        Account entity = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found id=" + id));
        accountMapper.updateEntity(entity, req);
        return accountMapper.toResponse(accountRepository.save(entity));
    }

    @Override
    public AccountResponse getById(String id) {
        Account entity = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found id=" + id));
        return accountMapper.toResponse(entity);
    }

    @Override
    public PageResponse<AccountResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<AccountResponse> data = accountRepository.findAll(pageable).map(accountMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(String id) {
        Account entity = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found id=" + id));
        accountRepository.delete(entity);
    }
}
