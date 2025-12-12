package com.vanmo.booking_app_be.controller;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.AccountCreateRequest;
import com.vanmo.booking_app_be.dto.request.AccountUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.AccountResponse;
import com.vanmo.booking_app_be.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ApiResponse<AccountResponse> create(@Valid @RequestBody AccountCreateRequest req) {
        return ApiResponse.ok(accountService.create(req), "Create account success");
    }

    @PutMapping("/{id}")
    public ApiResponse<AccountResponse> update(@PathVariable String id,
                                              @Valid @RequestBody AccountUpdateRequest req) {
        return ApiResponse.ok(accountService.update(id, req), "Update account success");
    }

    @GetMapping("/{id}")
    public ApiResponse<AccountResponse> getById(@PathVariable String id) {
        return ApiResponse.ok(accountService.getById(id), "Get account success");
    }

    @GetMapping
    public ApiResponse<PageResponse<AccountResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(accountService.getAll(page, size), "Get accounts success");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        accountService.delete(id);
        return ApiResponse.ok(null, "Delete account success");
    }
}
