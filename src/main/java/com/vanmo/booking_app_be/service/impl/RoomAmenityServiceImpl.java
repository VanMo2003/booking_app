package com.vanmo.booking_app_be.service.impl;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.vanmo.booking_app_be.dto.request.RoomAmenityCreateRequest;
import com.vanmo.booking_app_be.dto.request.RoomAmenityUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.RoomAmenityResponse;
import com.vanmo.booking_app_be.entity.Amenity;
import com.vanmo.booking_app_be.entity.Room;
import com.vanmo.booking_app_be.entity.RoomAmenity;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.RoomAmenityMapper;
import com.vanmo.booking_app_be.repository.AmenityRepository;
import com.vanmo.booking_app_be.repository.RoomAmenityRepository;
import com.vanmo.booking_app_be.repository.RoomRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.RoomAmenityService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomAmenityServiceImpl implements RoomAmenityService {

    private final RoomAmenityRepository roomAmenityRepository;
    private final RoomRepository roomRepository;
    private final AmenityRepository amenityRepository;
    private final RoomAmenityMapper roomAmenityMapper;
    private final AccessGuard accessGuard;

    @Override
    public boolean create(RoomAmenityCreateRequest req) {
        Room room = roomRepository
                .findById(req.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        accessGuard.requireBranchEditor(room.getHotel().getId());

        Amenity amenity = amenityRepository
                .findById(req.getAmenityId())
                .orElseThrow(() -> new ResourceNotFoundException("Amenity not found"));

        RoomAmenity entity = roomAmenityMapper.toEntity(req);
        entity.setRoom(room);
        entity.setAmenity(amenity);
        roomAmenityRepository.save(entity);
        return true;
    }

    @Override
    public RoomAmenityResponse update(Long id, RoomAmenityUpdateRequest req) {
        RoomAmenity entity = roomAmenityRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoomAmenity not found id=" + id));
        accessGuard.requireBranchEditor(entity.getRoom().getHotel().getId());
        roomAmenityMapper.updateEntity(entity, req);
        return roomAmenityMapper.toResponse(roomAmenityRepository.save(entity));
    }

    @Override
    public RoomAmenityResponse getById(Long id) {
        RoomAmenity entity = roomAmenityRepository
                .findById(id)
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
        RoomAmenity entity = roomAmenityRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoomAmenity not found id=" + id));
        accessGuard.requireBranchManagerOrAbove(entity.getRoom().getHotel().getId());
        roomAmenityRepository.delete(entity);
    }
}
