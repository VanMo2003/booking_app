package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.CustomerCreateRequest;
import com.vanmo.booking_app_be.dto.request.CustomerUpdateRequest;
import com.vanmo.booking_app_be.dto.response.CustomerResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

public interface CustomerService {
    CustomerResponse create(CustomerCreateRequest req);
    CustomerResponse update(Long id, CustomerUpdateRequest req);
    CustomerResponse getById(Long id);
    PageResponse<CustomerResponse> getAll(int page, int size);
    void delete(Long id);
}
