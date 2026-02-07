package com.vanmo.booking_app_be.controller;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.PayrollCreateRequest;
import com.vanmo.booking_app_be.dto.request.PayrollUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.PayrollResponse;
import com.vanmo.booking_app_be.service.PayrollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payrolls")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping
    public ApiResponse<PayrollResponse> create(@Valid @RequestBody PayrollCreateRequest req) {
        return ApiResponse.ok(payrollService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<PayrollResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody PayrollUpdateRequest req) {
        return ApiResponse.ok(payrollService.update(id, req));
    }

    @GetMapping("/{id}")
    public ApiResponse<PayrollResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(payrollService.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<PayrollResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(payrollService.getAll(page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        payrollService.delete(id);
        return ApiResponse.ok(null);
    }
}
