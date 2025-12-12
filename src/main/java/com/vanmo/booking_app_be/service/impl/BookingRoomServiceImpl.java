package com.vanmo.booking_app_be.service.impl;

import com.vanmo.booking_app_be.dto.request.BookingRoomCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingRoomUpdateRequest;
import com.vanmo.booking_app_be.dto.response.BookingRoomResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.BookingRoom;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.BookingRoomMapper;
import com.vanmo.booking_app_be.repository.BookingRoomRepository;
import com.vanmo.booking_app_be.service.BookingRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class BookingRoomServiceImpl implements BookingRoomService {

    private final BookingRoomRepository bookingRoomRepository;
    private final BookingRoomMapper bookingRoomMapper;

    @Override
    public BookingRoomResponse create(BookingRoomCreateRequest req) {
        BookingRoom entity = bookingRoomMapper.toEntity(req);
        return bookingRoomMapper.toResponse(bookingRoomRepository.save(entity));
    }

    @Override
    public BookingRoomResponse update(Long id, BookingRoomUpdateRequest req) {
        BookingRoom entity = bookingRoomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookingRoom not found id=" + id));
        bookingRoomMapper.updateEntity(entity, req);
        return bookingRoomMapper.toResponse(bookingRoomRepository.save(entity));
    }

    @Override
    public BookingRoomResponse getById(Long id) {
        BookingRoom entity = bookingRoomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookingRoom not found id=" + id));
        return bookingRoomMapper.toResponse(entity);
    }

    @Override
    public PageResponse<BookingRoomResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<BookingRoomResponse> data = bookingRoomRepository.findAll(pageable).map(bookingRoomMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(Long id) {
        BookingRoom entity = bookingRoomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookingRoom not found id=" + id));
        bookingRoomRepository.delete(entity);
    }
}
