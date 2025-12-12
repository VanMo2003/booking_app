package com.vanmo.booking_app_be.service.impl;

import com.vanmo.booking_app_be.dto.request.RoomAmenityCreateRequest;
import com.vanmo.booking_app_be.dto.request.RoomAmenityUpdateRequest;
import com.vanmo.booking_app_be.dto.response.RoomAmenityResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.RoomAmenity;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.RoomAmenityMapper;
import com.vanmo.booking_app_be.repository.RoomAmenityRepository;
import com.vanmo.booking_app_be.service.RoomAmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class RoomAmenityServiceImpl implements RoomAmenityService {

    private final RoomAmenityRepository roomAmenityRepository;
    private final RoomAmenityMapper roomAmenityMapper;

    @Override
    public RoomAmenityResponse create(RoomAmenityCreateRequest req) {
        RoomAmenity entity = roomAmenityMapper.toEntity(req);
        return roomAmenityMapper.toResponse(roomAmenityRepository.save(entity));
    }

    @Override
    public RoomAmenityResponse update(Long id, RoomAmenityUpdateRequest req) {
        RoomAmenity entity = roomAmenityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoomAmenity not found id=" + id));
        roomAmenityMapper.updateEntity(entity, req);
        return roomAmenityMapper.toResponse(roomAmenityRepository.save(entity));
    }

    @Override
    public RoomAmenityResponse getById(Long id) {
        RoomAmenity entity = roomAmenityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoomAmenity not found id=" + id));
        return roomAmenityMapper.toResponse(entity);
    }

    @Override
    public PageResponse<RoomAmenityResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<RoomAmenityResponse> data = roomAmenityRepository.findAll(pageable).map(roomAmenityMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(Long id) {
        RoomAmenity entity = roomAmenityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoomAmenity not found id=" + id));
        roomAmenityRepository.delete(entity);
    }
}
