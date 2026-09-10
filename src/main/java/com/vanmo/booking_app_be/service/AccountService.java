package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.AccountCreateRequest;
import com.vanmo.booking_app_be.dto.request.AccountUpdateRequest;
import com.vanmo.booking_app_be.dto.response.AccountResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

public interface AccountService {
    AccountResponse create(AccountCreateRequest req);

    AccountResponse update(String id, AccountUpdateRequest req);

    AccountResponse getById(String id);

    PageResponse<AccountResponse> getAll(int page, int size);

    void delete(String id);
}
