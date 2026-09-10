package com.vanmo.booking_app_be.service;

import java.util.List;

import com.vanmo.booking_app_be.dto.request.EmployeeCreateRequest;
import com.vanmo.booking_app_be.dto.request.EmployeeUpdateRequest;
import com.vanmo.booking_app_be.dto.response.EmployeeResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

public interface EmployeeService {
    EmployeeResponse create(EmployeeCreateRequest req);

    EmployeeResponse update(Long id, EmployeeUpdateRequest req);

    EmployeeResponse getById(Long id);

    List<EmployeeResponse> getAllByHotelId(Long hotelId);

    PageResponse<EmployeeResponse> getAll(int page, int size);

    void delete(Long id);
}
