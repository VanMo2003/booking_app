package com.vanmo.booking_app_be.service.impl;

import com.vanmo.booking_app_be.dto.request.PositionCreateRequest;
import com.vanmo.booking_app_be.dto.request.PositionUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PositionResponse;
import com.vanmo.booking_app_be.entity.Position;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.PositionMapper;
import com.vanmo.booking_app_be.repository.PositionRepository;
import com.vanmo.booking_app_be.service.PositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class PositionServiceImpl implements PositionService {

    private final PositionRepository positionRepository;
    private final PositionMapper positionMapper;

    @Override
    public PositionResponse create(PositionCreateRequest req) {
        Position entity = positionMapper.toEntity(req);
        PositionResponse positionResponse = positionMapper.toResponse(entity);
        positionRepository.save(entity);
        return positionResponse;
    }

    @Override
    public PositionResponse update(Long id, PositionUpdateRequest req) {
        Position entity = positionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found id=" + id));
        positionMapper.updateEntity(entity, req);
        return positionMapper.toResponse(positionRepository.save(entity));
    }

    @Override
    public PositionResponse getById(Long id) {
        Position entity = positionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found id=" + id));
        return positionMapper.toResponse(entity);
    }

    @Override
    public List<PositionResponse> getAll() {
        List<PositionResponse> positionResponses = positionRepository.findAll().stream().map(positionMapper::toResponse).toList();
        return positionResponses;
    }

    @Override
    public void delete(Long id) {
        Position entity = positionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found id=" + id));
        positionRepository.delete(entity);
    }
}
