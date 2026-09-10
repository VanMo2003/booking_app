package com.vanmo.booking_app_be.service.impl;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.vanmo.booking_app_be.dto.request.HotelImageCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelImageUpdateRequest;
import com.vanmo.booking_app_be.dto.response.HotelImageResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.HotelImage;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.HotelImageMapper;
import com.vanmo.booking_app_be.repository.HotelImageRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.HotelImageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HotelImageServiceImpl implements HotelImageService {

    private final HotelImageRepository hotelImageRepository;
    private final HotelImageMapper hotelImageMapper;
    private final AccessGuard accessGuard;

    @Override
    public HotelImageResponse create(HotelImageCreateRequest req) {
        Hotel hotel = accessGuard.requireBranchStaffOrAbove(req.getHotelId());
        HotelImage entity = hotelImageMapper.toEntity(req);
        entity.setHotel(hotel);
        return hotelImageMapper.toResponse(hotelImageRepository.save(entity));
    }

    @Override
    public HotelImageResponse update(Long id, HotelImageUpdateRequest req) {
        HotelImage entity = hotelImageRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HotelImage not found id=" + id));
        accessGuard.requireBranchStaffOrAbove(entity.getHotel().getId());
        hotelImageMapper.updateEntity(entity, req);
        return hotelImageMapper.toResponse(hotelImageRepository.save(entity));
    }

    @Override
    public HotelImageResponse getById(Long id) {
        HotelImage entity = hotelImageRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HotelImage not found id=" + id));
        return hotelImageMapper.toResponse(entity);
    }

    @Override
    public PageResponse<HotelImageResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<HotelImageResponse> data = hotelImageRepository.findAll(pageable).map(hotelImageMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(Long id) {
        HotelImage entity = hotelImageRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HotelImage not found id=" + id));
        accessGuard.requireBranchManagerOrAbove(entity.getHotel().getId());
        hotelImageRepository.delete(entity);
    }
}
