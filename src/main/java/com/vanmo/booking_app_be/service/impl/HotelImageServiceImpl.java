package com.vanmo.booking_app_be.service.impl;

import com.vanmo.booking_app_be.dto.request.HotelImageCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelImageUpdateRequest;
import com.vanmo.booking_app_be.dto.response.HotelImageResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.HotelImage;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.HotelImageMapper;
import com.vanmo.booking_app_be.repository.HotelImageRepository;
import com.vanmo.booking_app_be.service.HotelImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class HotelImageServiceImpl implements HotelImageService {

    private final HotelImageRepository hotelImageRepository;
    private final HotelImageMapper hotelImageMapper;

    @Override
    public HotelImageResponse create(HotelImageCreateRequest req) {
        HotelImage entity = hotelImageMapper.toEntity(req);
        return hotelImageMapper.toResponse(hotelImageRepository.save(entity));
    }

    @Override
    public HotelImageResponse update(Long id, HotelImageUpdateRequest req) {
        HotelImage entity = hotelImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HotelImage not found id=" + id));
        hotelImageMapper.updateEntity(entity, req);
        return hotelImageMapper.toResponse(hotelImageRepository.save(entity));
    }

    @Override
    public HotelImageResponse getById(Long id) {
        HotelImage entity = hotelImageRepository.findById(id)
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
        HotelImage entity = hotelImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HotelImage not found id=" + id));
        hotelImageRepository.delete(entity);
    }
}
