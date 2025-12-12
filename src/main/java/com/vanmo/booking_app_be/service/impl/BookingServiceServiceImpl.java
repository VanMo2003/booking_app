package com.vanmo.booking_app_be.service.impl;

import com.vanmo.booking_app_be.dto.request.BookingServiceCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingServiceUpdateRequest;
import com.vanmo.booking_app_be.dto.response.BookingServiceResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.BookingService;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.BookingServiceMapper;
import com.vanmo.booking_app_be.repository.BookingServiceRepository;
import com.vanmo.booking_app_be.service.BookingServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class BookingServiceServiceImpl implements BookingServiceService {

    private final BookingServiceRepository bookingServiceRepository;
    private final BookingServiceMapper bookingServiceMapper;

    @Override
    public BookingServiceResponse create(BookingServiceCreateRequest req) {
        BookingService entity = bookingServiceMapper.toEntity(req);
        return bookingServiceMapper.toResponse(bookingServiceRepository.save(entity));
    }

    @Override
    public BookingServiceResponse update(Long id, BookingServiceUpdateRequest req) {
        BookingService entity = bookingServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookingService not found id=" + id));
        bookingServiceMapper.updateEntity(entity, req);
        return bookingServiceMapper.toResponse(bookingServiceRepository.save(entity));
    }

    @Override
    public BookingServiceResponse getById(Long id) {
        BookingService entity = bookingServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookingService not found id=" + id));
        return bookingServiceMapper.toResponse(entity);
    }

    @Override
    public PageResponse<BookingServiceResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<BookingServiceResponse> data = bookingServiceRepository.findAll(pageable).map(bookingServiceMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(Long id) {
        BookingService entity = bookingServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookingService not found id=" + id));
        bookingServiceRepository.delete(entity);
    }
}
