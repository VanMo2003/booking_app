package com.vanmo.booking_app_be.service.impl;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.vanmo.booking_app_be.dto.request.BookingRoomCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingRoomUpdateRequest;
import com.vanmo.booking_app_be.dto.response.BookingRoomResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.BookingRoom;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.BookingRoomMapper;
import com.vanmo.booking_app_be.repository.BookingRepository;
import com.vanmo.booking_app_be.repository.BookingRoomRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.BookingRoomService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingRoomServiceImpl implements BookingRoomService {

    private final BookingRoomRepository bookingRoomRepository;
    private final BookingRepository bookingRepository;
    private final BookingRoomMapper bookingRoomMapper;
    private final AccessGuard accessGuard;

    @Override
    public BookingRoomResponse create(BookingRoomCreateRequest req) {
        // direct API entry point (BookingRoomController) - internal calls from
        // BookingServiceImpl.create() go through createBookingRoom() below instead,
        // since that caller has already run its own (customer-aware) access check.
        // Re-fetch the real Booking rather than trusting req.getBooking() - a client
        // calling this endpoint typically only sends {"booking": {"id": X}}, so any
        // other field (including .hotel) on that partial object is null.
        var booking = bookingRepository
                .findById(req.getBooking().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found id=" + req.getBooking().getId()));
        accessGuard.requireBranchStaffOrAbove(booking.getHotel().getId());
        BookingRoom entity = createBookingRoom(req);
        return bookingRoomMapper.toResponse(entity);
    }

    public BookingRoom createBookingRoom(BookingRoomCreateRequest req) {
        BookingRoom entity = bookingRoomMapper.toEntity(req);
        return bookingRoomRepository.save(entity);
    }

    @Override
    public BookingRoomResponse update(Long id, BookingRoomUpdateRequest req) {
        BookingRoom entity = bookingRoomRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookingRoom not found id=" + id));
        accessGuard.requireBranchStaffOrAbove(entity.getBooking().getHotel().getId());
        bookingRoomMapper.updateEntity(entity, req);
        return bookingRoomMapper.toResponse(bookingRoomRepository.save(entity));
    }

    @Override
    public BookingRoomResponse getById(Long id) {
        BookingRoom entity = bookingRoomRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookingRoom not found id=" + id));
        accessGuard.requireBranchStaffOrAbove(entity.getBooking().getHotel().getId());
        return bookingRoomMapper.toResponse(entity);
    }

    @Override
    public PageResponse<BookingRoomResponse> getAll(int page, int size) {
        accessGuard.requireAdmin();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<BookingRoomResponse> data = bookingRoomRepository.findAll(pageable).map(bookingRoomMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(Long id) {
        BookingRoom entity = bookingRoomRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookingRoom not found id=" + id));
        accessGuard.requireBranchManagerOrAbove(entity.getBooking().getHotel().getId());
        bookingRoomRepository.delete(entity);
    }
}
