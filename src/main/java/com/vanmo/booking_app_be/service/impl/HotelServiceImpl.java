package com.vanmo.booking_app_be.service.impl;

import com.vanmo.booking_app_be.dto.request.HotelCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelUpdateRequest;
import com.vanmo.booking_app_be.dto.response.*;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Room;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.AmenityMapper;
import com.vanmo.booking_app_be.mapper.HotelMapper;
import com.vanmo.booking_app_be.mapper.RoomMapper;
import com.vanmo.booking_app_be.mapper.ServiceMapper;
import com.vanmo.booking_app_be.repository.*;
import com.vanmo.booking_app_be.service.AmenityService;
import com.vanmo.booking_app_be.service.HotelService;
import com.vanmo.booking_app_be.service.ServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final AccountRepository accountRepository;
    private final HotelMapper hotelMapper;
    private final ServiceService serviceService;
    private final AmenityService amenityService;
    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;
    private final ServiceRepository serviceRepository;
    private final AmenityRepository amenityRepository;
    private final ServiceMapper serviceMapper;
    private final AmenityMapper amenityMapper;

    @Override
    public ListHotelResponse create(HotelCreateRequest req) {
        Hotel hotel = hotelMapper.toEntity(req);
        Account acc = accountRepository.findById(req.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found id=" + req.getAccountId()));
        hotel.setAccount(acc);
        hotel.setRating(0.0);
        return hotelMapper.toResponse(hotelRepository.save(hotel));
    }

    @Override
    public ListHotelResponse update(Long id, HotelUpdateRequest req) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id=" + id));
        hotelMapper.updateEntity(hotel, req);
        return hotelMapper.toResponse(hotelRepository.save(hotel));
    }

    @Override
    public HotelDetailResponse getById(Long id) {

        List<RoomResponse> roomResponses = roomRepository.findAllByHotel_Id(id).stream().map(roomMapper::toResponse).toList();
        List<ServiceResponse> serviceResponses = serviceRepository.findAllByHotel_Id(id).stream().map(serviceMapper::toResponse).toList();
        List<AmenityResponse> amenityResponses = amenityRepository.findAllByHotel_Id(id).stream().map(amenityMapper::toResponse).toList();


        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id=" + id));

        HotelDetailResponse hotelDetailResponse = hotelMapper.toDetailResponse(hotel);
        hotelDetailResponse.setRooms(roomResponses);
        hotelDetailResponse.setServices(serviceResponses);
        hotelDetailResponse.setAmenities(amenityResponses);

        return hotelDetailResponse;
    }

    @Override
    public PageResponse<ListHotelResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("onCreate"));
        Page<ListHotelResponse> data = hotelRepository.findAll(pageable).map(this::toResponseFull);
        return PageResponse.from(data);
    }

    @Override
    public List<ListHotelResponse> getAllByCategory(String name) {
        List<Hotel> hotels = hotelRepository.findAllByCategory(name);
        return hotels.stream().map(this::toResponseFull).toList();
    }

    @Override
    public void delete(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id=" + id));
        hotelRepository.delete(hotel);
    }

    private ListHotelResponse toResponseFull(Hotel hotel) {
        ListHotelResponse listHotelResponse = hotelMapper.toResponse(hotel);
        List<AmenityResponse> amenityResponses = amenityService.getAllByHotel(hotel.getId());
        listHotelResponse.setAmenities(amenityResponses);

        List<ServiceResponse> serviceResponses = serviceService.getAllByHotel(hotel.getId());
        listHotelResponse.setServices(serviceResponses);

        return listHotelResponse;
    }
}
