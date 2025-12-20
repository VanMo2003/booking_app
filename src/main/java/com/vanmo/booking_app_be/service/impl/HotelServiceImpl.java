package com.vanmo.booking_app_be.service.impl;

import com.vanmo.booking_app_be.dto.request.HotelCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelUpdateRequest;
import com.vanmo.booking_app_be.dto.response.HotelResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.HotelMapper;
import com.vanmo.booking_app_be.repository.AccountRepository;
import com.vanmo.booking_app_be.repository.HotelRepository;
import com.vanmo.booking_app_be.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final AccountRepository accountRepository;
    private final HotelMapper hotelMapper;

    @Override
    public HotelResponse create(HotelCreateRequest req) {
        Hotel hotel = hotelMapper.toEntity(req);
        Account acc = accountRepository.findById(req.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found id=" + req.getAccountId()));
        hotel.setAccount(acc);
        hotel.setRating(0.0);
        return hotelMapper.toResponse(hotelRepository.save(hotel));
    }

    @Override
    public HotelResponse update(Long id, HotelUpdateRequest req) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id=" + id));
        hotelMapper.updateEntity(hotel, req);
        return hotelMapper.toResponse(hotelRepository.save(hotel));
    }

    @Override
    public HotelResponse getById(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id=" + id));
        return hotelMapper.toResponse(hotel);
    }

    @Override
    public PageResponse<HotelResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("onCreate").descending());
        Page<HotelResponse> data = hotelRepository.findAll(pageable).map(hotelMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public List<HotelResponse> getAllByCategory(String name) {
        List<Hotel> hotels = hotelRepository.findAllByCategory(name);
        return hotels.stream().map(hotelMapper::toResponse).toList();
    }

    @Override
    public void delete(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id=" + id));
        hotelRepository.delete(hotel);
    }
}
