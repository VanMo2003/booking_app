package com.vanmo.booking_app_be.service.impl;

import com.vanmo.booking_app_be.dto.request.BookingCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingUpdateRequest;
import com.vanmo.booking_app_be.dto.response.BookingResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.Booking;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.BookingMapper;
import com.vanmo.booking_app_be.repository.BookingRepository;
import com.vanmo.booking_app_be.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;

    @Override
    public BookingResponse create(BookingCreateRequest req) {
        Booking entity = bookingMapper.toEntity(req);
        return bookingMapper.toResponse(bookingRepository.save(entity));
    }

    @Override
    public BookingResponse update(Long id, BookingUpdateRequest req) {
        Booking entity = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found id=" + id));
        bookingMapper.updateEntity(entity, req);
        return bookingMapper.toResponse(bookingRepository.save(entity));
    }

    @Override
    public BookingResponse getById(Long id) {
        Booking entity = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found id=" + id));
        return bookingMapper.toResponse(entity);
    }

    @Override
    public PageResponse<BookingResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<BookingResponse> data = bookingRepository.findAll(pageable).map(bookingMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(Long id) {
        Booking entity = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found id=" + id));
        bookingRepository.delete(entity);
    }
}
