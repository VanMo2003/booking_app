package com.vanmo.booking_app_be.service.impl;

import java.util.List;

import org.springframework.data.domain.*;

import com.vanmo.booking_app_be.dto.request.ServiceCreateRequest;
import com.vanmo.booking_app_be.dto.request.ServiceUpdateRequest;
import com.vanmo.booking_app_be.dto.response.ServiceResponse;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Service;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.ServiceMapper;
import com.vanmo.booking_app_be.repository.HotelRepository;
import com.vanmo.booking_app_be.repository.ServiceRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.ServiceService;

import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceServiceImpl implements ServiceService {

    private final ServiceRepository serviceRepository;
    private final ServiceMapper serviceMapper;
    private final HotelRepository hotelRepository;
    private final AccessGuard accessGuard;

    @Override
    public ServiceResponse create(ServiceCreateRequest req) {
        Hotel hotel = accessGuard.requireBranchEditor(req.getHotelId());
        Service entity = serviceMapper.toEntity(req);
        entity.setHotel(hotel);
        return serviceMapper.toResponse(serviceRepository.save(entity));
    }

    @Override
    public ServiceResponse update(Long id, ServiceUpdateRequest req) {
        Service entity = serviceRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found id=" + id));
        accessGuard.requireBranchEditor(entity.getHotel().getId());
        serviceMapper.updateEntity(entity, req);
        return serviceMapper.toResponse(serviceRepository.save(entity));
    }

    @Override
    public ServiceResponse getById(Long id) {
        Service entity = serviceRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found id=" + id));
        return serviceMapper.toResponse(entity);
    }

    @Override
    public List<ServiceResponse> getAllByHotel(Long hotelId) {
        List<Service> services = serviceRepository.findByHotelId(hotelId);
        List<ServiceResponse> responses =
                services.stream().map(serviceMapper::toResponse).toList();
        return responses;
    }

    @Override
    public void delete(Long id) {
        Service entity = serviceRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found id=" + id));
        accessGuard.requireBranchManagerEditor(entity.getHotel().getId());
        serviceRepository.delete(entity);
    }
}
