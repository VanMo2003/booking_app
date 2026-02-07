package com.vanmo.booking_app_be.service.impl;

import com.vanmo.booking_app_be.dto.request.RoomTypeCreateRequest;
import com.vanmo.booking_app_be.dto.request.RoomTypeUpdateRequest;
import com.vanmo.booking_app_be.dto.response.RoomTypeResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.RoomType;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.RoomTypeMapper;
import com.vanmo.booking_app_be.repository.RoomTypeRepository;
import com.vanmo.booking_app_be.service.RoomTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomTypeMapper roomTypeMapper;

    @Override
    public RoomTypeResponse create(RoomTypeCreateRequest req) {
        RoomType entity = roomTypeMapper.toEntity(req);
        return roomTypeMapper.toResponse(roomTypeRepository.save(entity));
    }

    @Override
    public RoomTypeResponse update(Long id, RoomTypeUpdateRequest req) {
        RoomType entity = roomTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoomType not found id=" + id));
        roomTypeMapper.updateEntity(entity, req);
        return roomTypeMapper.toResponse(roomTypeRepository.save(entity));
    }

    @Override
    public RoomTypeResponse getById(Long id) {
        RoomType entity = roomTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoomType not found id=" + id));
        return roomTypeMapper.toResponse(entity);
    }

    @Override
    public List<RoomTypeResponse> getAll() {
        List<RoomTypeResponse> romRoomTypeResponses = roomTypeRepository.findAll().stream().map(roomTypeMapper::toResponse).toList();
        return romRoomTypeResponses;
    }

    @Override
    public void delete(Long id) {
        RoomType entity = roomTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoomType not found id=" + id));
        roomTypeRepository.delete(entity);
    }
}
