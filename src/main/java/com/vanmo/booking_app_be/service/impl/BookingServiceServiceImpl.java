package com.vanmo.booking_app_be.service.impl;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.vanmo.booking_app_be.dto.request.BookingServiceCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingServiceUpdateRequest;
import com.vanmo.booking_app_be.dto.response.BookingServiceResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.BookingService;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.BookingServiceMapper;
import com.vanmo.booking_app_be.repository.BookingRepository;
import com.vanmo.booking_app_be.repository.BookingServiceRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.BookingServiceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingServiceServiceImpl implements BookingServiceService {

    private final BookingServiceRepository bookingServiceRepository;
    private final BookingRepository bookingRepository;
    private final BookingServiceMapper bookingServiceMapper;
    private final AccessGuard accessGuard;

    @Override
    public BookingServiceResponse create(BookingServiceCreateRequest req) {
        // direct API entry point (BookingServiceController) - internal calls from
        // BookingServiceImpl.create() go through createBookingService() below instead,
        // since that caller has already run its own (customer-aware) access check.
        // Re-fetch the real Booking rather than trusting req.getBooking() - a client
        // calling this endpoint typically only sends {"booking": {"id": X}}, so any
        // other field (including .hotel) on that partial object is null.
        var booking = bookingRepository
                .findById(req.getBooking().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found id=" + req.getBooking().getId()));
        accessGuard.requireBranchEditor(booking.getHotel().getId());
        BookingService entity = createBookingService(req);
        return bookingServiceMapper.toResponse(entity);
    }

    public BookingService createBookingService(BookingServiceCreateRequest req) {
        BookingService entity = bookingServiceMapper.toEntity(req);
        return bookingServiceRepository.save(entity);
    }

    @Override
    public BookingServiceResponse update(Long id, BookingServiceUpdateRequest req) {
        BookingService entity = bookingServiceRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookingService not found id=" + id));
        accessGuard.requireBranchEditor(entity.getBooking().getHotel().getId());
        bookingServiceMapper.updateEntity(entity, req);
        return bookingServiceMapper.toResponse(bookingServiceRepository.save(entity));
    }

    @Override
    public BookingServiceResponse getById(Long id) {
        BookingService entity = bookingServiceRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookingService not found id=" + id));
        accessGuard.requireBranchStaffOrAbove(entity.getBooking().getHotel().getId());
        return bookingServiceMapper.toResponse(entity);
    }

    @Override
    public PageResponse<BookingServiceResponse> getAll(int page, int size) {
        accessGuard.requireAdmin();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<BookingServiceResponse> data =
                bookingServiceRepository.findAll(pageable).map(bookingServiceMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(Long id) {
        BookingService entity = bookingServiceRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookingService not found id=" + id));
        accessGuard.requireBranchManagerEditor(entity.getBooking().getHotel().getId());
        bookingServiceRepository.delete(entity);
    }
}
