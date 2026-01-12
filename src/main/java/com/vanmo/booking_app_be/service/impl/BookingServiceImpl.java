package com.vanmo.booking_app_be.service.impl;

import com.vanmo.booking_app_be.dto.request.BookingCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingRoomCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingServiceCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingUpdateRequest;
import com.vanmo.booking_app_be.dto.response.*;
import com.vanmo.booking_app_be.entity.*;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.*;
import com.vanmo.booking_app_be.repository.*;
import com.vanmo.booking_app_be.service.BookingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.ArrayList;
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
    private final BookingRoomRepository bookingRoomRepository;
    private final BookingRoomMapper bookingRoomMapper;
    private final BookingServiceMapper bookingServiceMapper;

    @Override
    @Transactional
    public BookingResponse create(BookingCreateRequest req) {
        Hotel hotel = hotelRepository.findById(req.getHotelId()).orElse(null);
        Customer customer = customerRepository.findById(req.getHotelId()).orElse(null);
        Booking booking = bookingMapper.toEntity(req);
        booking.setHotel(hotel);
        booking.setCustomer(customer);
        bookingRepository.save(booking);
        BookingResponse bookingResponse = bookingMapper.toResponse(booking);
        List<BookingRoomResponse> bookingRoomResponses = new ArrayList<>();
        List<BookingServiceResponse> bookingServiceResponses = new ArrayList<>();

        for (Long room : req.getRooms()){
            BookingRoomCreateRequest roomCreateRequest = new BookingRoomCreateRequest();
            roomCreateRequest.setBooking(booking);
            roomCreateRequest.setRoom(roomRepository.findById(room).orElse(null));
            BookingRoom bookingRoom = bookingRoomServiceImpl.createBookingRoom(roomCreateRequest);

            BookingRoomResponse bookingRoomResponse = bookingRoomMapper.toResponse(bookingRoom);
            RoomInfoResponse roomInfoResponse = roomMapper.toInfoResponse(bookingRoom.getRoom());
            bookingRoomResponse.setRoomInfo(roomInfoResponse);
            bookingRoomResponses.add(bookingRoomResponse);
        }

        for (Long service : req.getServices()){
            BookingServiceCreateRequest serviceCreateRequest = new BookingServiceCreateRequest();
            serviceCreateRequest.setBooking(booking);
            serviceCreateRequest.setService(serviceRepository.findById(service).orElse(null));
            serviceCreateRequest.setQuantity(1L);
            serviceCreateRequest.setTotal(BigDecimal.valueOf(0));
            com.vanmo.booking_app_be.entity.BookingService bookingService = bookingServiceServiceImpl.createBookingService(serviceCreateRequest);

            BookingServiceResponse bookingServiceResponse = bookingServiceMapper.toResponse(bookingService);
            ServiceInfoResponse serviceInfoResponse = serviceMapper.toInfoResponse(bookingService.getService());
            bookingServiceResponse.setServiceInfo(serviceInfoResponse);
            bookingServiceResponses.add(bookingServiceResponse);
        }

        bookingResponse.setRooms(bookingRoomResponses);
        bookingResponse.setServices(bookingServiceResponses);

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
    public List<ListBookingResponse> getAll(Long hotelId) {
        List<ListBookingResponse> bookingResponses = bookingRepository.findAllByHotel_Id(hotelId).stream().map(booking -> {
            ListBookingResponse bookingResponse = bookingMapper.toListResponse(booking);
            List<BookingRoomResponse> bookingRoomResponses = booking.getBookingRooms().stream().map(bookingRoom -> {
                BookingRoomResponse bookingRoomResponse = bookingRoomMapper.toResponse(bookingRoom);
                RoomInfoResponse roomInfoResponse = roomMapper.toInfoResponse(bookingRoom.getRoom());

                bookingRoomResponse.setRoomInfo(roomInfoResponse);

                return bookingRoomResponse;
            }).toList();

            List<BookingServiceResponse> bookingServiceResponses = booking.getBookingServices().stream().map(bookingService -> {
                BookingServiceResponse bookingServiceResponse = bookingServiceMapper.toResponse(bookingService);

                ServiceInfoResponse serviceInfoResponse = serviceMapper.toInfoResponse(bookingService.getService());
                bookingServiceResponse.setServiceInfo(serviceInfoResponse);

                return  bookingServiceResponse;
            }).toList();

            bookingResponse.setRooms(bookingRoomResponses);
            bookingResponse.setServices(bookingServiceResponses);

            return bookingResponse;
        }).toList();
        return bookingResponses;
    }

    @Override
    public void delete(Long id) {
        Booking entity = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found id=" + id));
        bookingRepository.delete(entity);
    }
}
