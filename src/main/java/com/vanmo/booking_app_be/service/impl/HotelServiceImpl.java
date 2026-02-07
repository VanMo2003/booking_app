package com.vanmo.booking_app_be.service.impl;

import com.vanmo.booking_app_be.dto.request.HotelCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelUpdateRequest;
import com.vanmo.booking_app_be.dto.response.*;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.HotelImage;
import com.vanmo.booking_app_be.entity.Room;
import com.vanmo.booking_app_be.entity.enums.HotelStatus;
import com.vanmo.booking_app_be.entity.enums.RoomStatus;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.*;
import com.vanmo.booking_app_be.repository.*;
import com.vanmo.booking_app_be.service.AmenityService;
import com.vanmo.booking_app_be.service.FileStorageService;
import com.vanmo.booking_app_be.service.HotelService;
import com.vanmo.booking_app_be.service.ServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
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
    private final FileStorageService fileStorageService;
    private final HotelImageRepository hotelImageRepository;
    private final BookingRoomRepository bookingRoomRepository;

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
    public HotelDetailResponse getById(Long id, LocalDate checkinDate, LocalDate checkoutDate) {

        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id=" + id));


        List<ServiceResponse> serviceResponses = serviceRepository.findAllByHotel_Id(id)
                .stream().map(serviceMapper::toResponse).toList();

        List<AmenityResponse> amenityResponses = amenityRepository.findAllByHotel_Id(id)
                .stream().map(amenityMapper::toResponse).toList();

        List<String> hotelImages = hotelImageRepository.findAllByHotel_Id(id)
                .stream().map(HotelImage::getPathImage).toList();


        List<Room> rooms = roomRepository.findAllByHotel_Id(id);

        List<Long> bookedRoomIds = List.of();
        boolean hasDateFilter = (checkinDate != null && checkoutDate != null);

        if (hasDateFilter) {
            bookedRoomIds = bookingRoomRepository.findBookedRoomIds(checkinDate, checkoutDate);
        } else if (checkinDate != null || checkoutDate != null) {
            throw new IllegalArgumentException("checkinDate và checkoutDate phải đi cùng nhau");
        }

        List<Long> finalBookedRoomIds = bookedRoomIds;

        List<RoomResponse> roomResponses = rooms.stream().map(room -> {
            RoomResponse resp = roomMapper.toResponse(room);

            if (hasDateFilter) {
                resp.setStatus(calcRoomStatus(room, finalBookedRoomIds));
            }

            return resp;
        }).toList();

        HotelDetailResponse hotelDetailResponse = hotelMapper.toDetailResponse(hotel);
        hotelDetailResponse.setRooms(roomResponses);
        hotelDetailResponse.setServices(serviceResponses);
        hotelDetailResponse.setAmenities(amenityResponses);
        hotelDetailResponse.setImages(hotelImages);

        return hotelDetailResponse;
    }

    private RoomStatus calcRoomStatus(Room room, List<Long> bookedRoomIds) {
        RoomStatus current = room.getStatus();

        if (current == RoomStatus.MAINTENANCE) return RoomStatus.MAINTENANCE;
        if (current == RoomStatus.OCCUPIED) return RoomStatus.OCCUPIED;

        if (bookedRoomIds.contains(room.getId())) return RoomStatus.BOOKED;

        return RoomStatus.AVAILABLE;
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
    public List<String> uploadImages(Long hotelId, List<MultipartFile> files) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id=" + hotelId));

        List<String> urls = files.stream().map(fileStorageService::uploadHotelImage).toList();

        List<HotelImage> images = urls.stream().map(url ->
                HotelImage.builder()
                        .pathImage(url)
                        .hotel(hotel)
                        .build()
        ).toList();

        hotelImageRepository.saveAll(images);

        return urls;
    }

    @Override
    public void delete(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id=" + id));
        hotelRepository.delete(hotel);
    }

    @Override
    public List<ListHotelResponse> searchByDate(LocalDate checkinDate, LocalDate checkoutDate) {
        List<Hotel> hotels = hotelRepository.findAll();
        List<Long> bookedRoomIds =
                bookingRoomRepository.findBookedRoomIds(checkinDate, checkoutDate);

        return hotels.stream().map(hotel -> {

            List<Room> rooms = roomRepository.findAllByHotel_Id(hotel.getId());

            boolean hasAvailableRoom = rooms.stream()
                    .anyMatch(room -> !bookedRoomIds.contains(room.getId()));

            return toResponseFullHasStatus(hotel, hasAvailableRoom);
        }).toList();

    }

    private ListHotelResponse toResponseFull(Hotel hotel) {
        ListHotelResponse listHotelResponse = hotelMapper.toResponse(hotel);
        List<AmenityResponse> amenityResponses = amenityService.getAllByHotel(hotel.getId());
        listHotelResponse.setAmenities(amenityResponses);

        List<ServiceResponse> serviceResponses = serviceService.getAllByHotel(hotel.getId());
        listHotelResponse.setServices(serviceResponses);

        return listHotelResponse;
    }

    private ListHotelResponse toResponseFullHasStatus(Hotel hotel, boolean hasAvailableRoom) {
        ListHotelResponse listHotelResponse = hotelMapper.toResponse(hotel);

        if (!hotel.isActive()) {
            listHotelResponse.setStatus(HotelStatus.INACTIVE);
        } else if (!hasAvailableRoom) {
            listHotelResponse.setStatus(HotelStatus.FULL);
        } else {
            listHotelResponse.setStatus(HotelStatus.AVAILABLE);
        }

        List<AmenityResponse> amenityResponses = amenityService.getAllByHotel(hotel.getId());
        listHotelResponse.setAmenities(amenityResponses);

        List<ServiceResponse> serviceResponses = serviceService.getAllByHotel(hotel.getId());
        listHotelResponse.setServices(serviceResponses);

        return listHotelResponse;
    }
}
