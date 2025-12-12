package com.vanmo.booking_app_be.service.impl;

import com.vanmo.booking_app_be.dto.request.AmenityCreateRequest;
import com.vanmo.booking_app_be.dto.request.AmenityUpdateRequest;
import com.vanmo.booking_app_be.dto.response.AmenityResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.Amenity;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.AmenityMapper;
import com.vanmo.booking_app_be.repository.AmenityRepository;
import com.vanmo.booking_app_be.repository.HotelRepository;
import com.vanmo.booking_app_be.service.AmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AmenityServiceImpl implements AmenityService {

    private final AmenityRepository amenityRepository;
    private final HotelRepository hotelRepository;
    private final AmenityMapper amenityMapper;

    @Override
    public AmenityResponse create(AmenityCreateRequest req) {
        Amenity entity = amenityMapper.toEntity(req);
        if (req.getHotelId() != null) {
            Hotel h = hotelRepository.findById(req.getHotelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Hotel not found id=" + req.getHotelId()));
            entity.setHotel(h);
        }
        return amenityMapper.toResponse(amenityRepository.save(entity));
    }

    @Override
    public AmenityResponse update(Long id, AmenityUpdateRequest req) {
        Amenity entity = amenityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity not found id=" + id));
        amenityMapper.updateEntity(entity, req);
        return amenityMapper.toResponse(amenityRepository.save(entity));
    }

    @Override
    public AmenityResponse getById(Long id) {
        Amenity entity = amenityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity not found id=" + id));
        return amenityMapper.toResponse(entity);
    }

    @Override
    public PageResponse<AmenityResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<AmenityResponse> data = amenityRepository.findAll(pageable).map(amenityMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(Long id) {
        Amenity entity = amenityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity not found id=" + id));
        amenityRepository.delete(entity);
    }
}
