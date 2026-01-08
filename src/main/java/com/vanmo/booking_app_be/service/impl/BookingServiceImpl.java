package com.vanmo.booking_app_be.service.impl;

import com.vanmo.booking_app_be.dto.request.BookingCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingRoomCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingServiceCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingUpdateRequest;
import com.vanmo.booking_app_be.dto.response.BookingResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.*;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.*;
import com.vanmo.booking_app_be.repository.*;
import com.vanmo.booking_app_be.service.BookingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;


@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingRoomServiceImpl bookingRoomServiceImpl;
    private final BookingServiceServiceImpl bookingServiceServiceImpl;
    private final BookingMapper bookingMapper;
    private final HotelRepository hotelRepository;
    private final CustomerRepository customerRepository;
    private final RoomRepository roomRepository;
    private final ServiceRepository serviceRepository;
    private final HotelMapper hotelMapper;
    private final CustomerMapper customerMapper;
    private final RoomMapper roomMapper;
    private final ServiceMapper serviceMapper;

    @Override
    @Transactional
    public BookingResponse create(BookingCreateRequest req) {
        Hotel hotel = hotelRepository.findById(req.getHotelId()).orElse(null);
        Customer customer = customerRepository.findById(req.getHotelId()).orElse(null);
        Booking booking = bookingMapper.toEntity(req);
        booking.setHotel(hotel);
        booking.setCustomer(customer);
        bookingRepository.save(booking);

        for (Long room : req.getRooms()){
            BookingRoomCreateRequest roomCreateRequest = new BookingRoomCreateRequest();
            roomCreateRequest.setBooking(booking);
            roomCreateRequest.setRoom(roomRepository.findById(room).orElse(null));
            bookingRoomServiceImpl.createBookingRoom(roomCreateRequest);
        }

        for (Long service : req.getServices()){
            BookingServiceCreateRequest serviceCreateRequest = new BookingServiceCreateRequest();
            serviceCreateRequest.setBooking(booking);
            serviceCreateRequest.setService(serviceRepository.findById(service).orElse(null));
            serviceCreateRequest.setQuantity(1L);
            serviceCreateRequest.setTotal(BigDecimal.valueOf(0));
            bookingServiceServiceImpl.createBookingService(serviceCreateRequest);
        }


        List<Room> rooms = roomRepository.findAllById(req.getRooms());
        List<Service> services = serviceRepository.findAllById(req.getServices());

        BookingResponse bookingResponse = bookingMapper.toResponse(booking);
        bookingResponse.setHotel(hotelMapper.toResponse(hotel));
        bookingResponse.setCustomer(customerMapper.toResponse(customer));
        bookingResponse.setRooms(rooms.stream().map(roomMapper::toResponse).toList());
        bookingResponse.setServices(services.stream().map(serviceMapper::toResponse).toList());

        return bookingResponse;
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
