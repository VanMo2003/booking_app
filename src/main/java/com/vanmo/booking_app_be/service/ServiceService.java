package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.ServiceCreateRequest;
import com.vanmo.booking_app_be.dto.request.ServiceUpdateRequest;
import com.vanmo.booking_app_be.dto.response.ServiceResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

public interface ServiceService {
    ServiceResponse create(ServiceCreateRequest req);
    ServiceResponse update(Long id, ServiceUpdateRequest req);
    ServiceResponse getById(Long id);
    PageResponse<ServiceResponse> getAll(int page, int size);
    void delete(Long id);
}
