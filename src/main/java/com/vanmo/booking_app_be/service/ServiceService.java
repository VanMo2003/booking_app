package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.ServiceCreateRequest;
import com.vanmo.booking_app_be.dto.request.ServiceUpdateRequest;
import com.vanmo.booking_app_be.dto.response.ServiceResponse;

import java.util.List;

public interface ServiceService {
    ServiceResponse create(ServiceCreateRequest req);
    ServiceResponse update(Long id, ServiceUpdateRequest req);
    ServiceResponse getById(Long id);
    List<ServiceResponse> getAllByHotel(Long hotelId);
    void delete(Long id);
}
