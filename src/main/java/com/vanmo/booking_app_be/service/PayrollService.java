package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.PayrollCreateRequest;
import com.vanmo.booking_app_be.dto.request.PayrollUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PayrollResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

public interface PayrollService {
    PayrollResponse create(PayrollCreateRequest req);
    PayrollResponse update(Long id, PayrollUpdateRequest req);
    PayrollResponse getById(Long id);
    PageResponse<PayrollResponse> getAll(int page, int size);
    void delete(Long id);
}
