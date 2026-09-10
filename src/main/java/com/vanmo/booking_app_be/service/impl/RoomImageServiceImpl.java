package com.vanmo.booking_app_be.service.impl;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.vanmo.booking_app_be.dto.request.RoomImageCreateRequest;
import com.vanmo.booking_app_be.dto.request.RoomImageUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.RoomImageResponse;
import com.vanmo.booking_app_be.entity.RoomImage;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.RoomImageMapper;
import com.vanmo.booking_app_be.repository.RoomImageRepository;
import com.vanmo.booking_app_be.service.RoomImageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomImageServiceImpl implements RoomImageService {

    private final RoomImageRepository roomImageRepository;
    private final RoomImageMapper roomImageMapper;

    @Override
    public RoomImageResponse create(RoomImageCreateRequest req) {
        RoomImage entity = roomImageMapper.toEntity(req);
        return roomImageMapper.toResponse(roomImageRepository.save(entity));
    }

    @Override
    public RoomImageResponse update(Long id, RoomImageUpdateRequest req) {
        RoomImage entity = roomImageRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoomImage not found id=" + id));
        roomImageMapper.updateEntity(entity, req);
        return roomImageMapper.toResponse(roomImageRepository.save(entity));
    }

    @Override
    public RoomImageResponse getById(Long id) {
        RoomImage entity = roomImageRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoomImage not found id=" + id));
        return roomImageMapper.toResponse(entity);
    }

    @Override
    public PageResponse<RoomImageResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<RoomImageResponse> data = roomImageRepository.findAll(pageable).map(roomImageMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(Long id) {
        RoomImage entity = roomImageRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoomImage not found id=" + id));
        roomImageRepository.delete(entity);
    }
}
