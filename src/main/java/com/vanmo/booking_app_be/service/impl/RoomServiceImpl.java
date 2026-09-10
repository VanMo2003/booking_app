package com.vanmo.booking_app_be.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.vanmo.booking_app_be.dto.request.RoomCreateRequest;
import com.vanmo.booking_app_be.dto.request.RoomUpdateRequest;
import com.vanmo.booking_app_be.dto.response.AmenityResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.RoomDetailResponse;
import com.vanmo.booking_app_be.dto.response.RoomResponse;
import com.vanmo.booking_app_be.entity.*;
import com.vanmo.booking_app_be.entity.enums.RoomStatus;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.AmenityMapper;
import com.vanmo.booking_app_be.mapper.RoomMapper;
import com.vanmo.booking_app_be.repository.*;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.FileStorageService;
import com.vanmo.booking_app_be.service.RoomService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomMapper roomMapper;
    private final RoomAmenityRepository roomAmenityRepository;
    private final AmenityMapper amenityMapper;
    private final FileStorageService fileStorageService;
    private final RoomImageRepository roomImageRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final AccessGuard accessGuard;

    @Override
    public RoomResponse create(RoomCreateRequest req) {
        Hotel hotel = accessGuard.requireBranchStaffOrAbove(req.getHotelId());

        boolean checkRoomNumber = roomRepository.existsByHotelAndRoomNumber(hotel, req.getRoomNumber());
        if (checkRoomNumber) {
            throw new ResourceNotFoundException("Room number already exists");
        }

        RoomType roomType = roomTypeRepository
                .findById(req.getRoomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("RoomType not found id=" + req.getRoomTypeId()));

        Room room = roomMapper.toEntity(req);
        room.setHotel(hotel);
        room.setRoomType(roomType);

        return roomMapper.toResponse(roomRepository.save(room));
    }

    @Override
    public RoomResponse update(Long id, RoomUpdateRequest req) {
        Room room =
                roomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Room not found id=" + id));
        accessGuard.requireBranchStaffOrAbove(room.getHotel().getId());

        RoomType roomType = roomTypeRepository
                .findById(req.getRoomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("RoomType not found id=" + req.getRoomTypeId()));

        roomMapper.updateEntity(room, req);
        room.setRoomType(roomType);

        return roomMapper.toResponse(roomRepository.save(room));
    }

    @Override
    public RoomDetailResponse getById(Long id) {
        List<AmenityResponse> amenityResponses = roomAmenityRepository.findAllByRoomId(id).stream()
                .map(roomAmenity -> amenityMapper.toResponse(roomAmenity.getAmenity()))
                .toList();
        List<String> roomImages = roomImageRepository.findAllByRoom_Id(id).stream()
                .map(RoomImage::getPathImage)
                .toList();

        Room room =
                roomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Room not found id=" + id));

        RoomDetailResponse roomDetailResponse = roomMapper.toDetailResponse(room);
        roomDetailResponse.setAmenities(amenityResponses);
        roomDetailResponse.setImages(roomImages);

        return roomDetailResponse;
    }

    @Override
    public PageResponse<RoomResponse> getAllByHotel(Long hotelId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("onCreate"));
        Page<RoomResponse> data =
                roomRepository.findByHotelId(hotelId, pageable).map(roomMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public List<String> uploadImages(Long roomId, List<MultipartFile> files) {
        Room room = roomRepository
                .findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id=" + roomId));
        accessGuard.requireBranchStaffOrAbove(room.getHotel().getId());

        List<String> urls =
                files.stream().map(fileStorageService::uploadHotelImage).toList();

        List<RoomImage> images = urls.stream()
                .map(url -> RoomImage.builder().pathImage(url).room(room).build())
                .toList();

        roomImageRepository.saveAll(images);

        return urls;
    }

    @Override
    public void delete(Long id) {
        Room room =
                roomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Room not found id=" + id));
        accessGuard.requireBranchManagerOrAbove(room.getHotel().getId());
        //        List<RoomAmenity> roomAmenities = roomAmenityRepository.findAllByRoom(room);
        //        roomAmenityRepository.deleteAll(roomAmenities);
        roomRepository.delete(room);
    }

    @Override
    public List<RoomResponse> getRoomsByDate(Long hotelId, LocalDate checkinDate, LocalDate checkoutDate) {
        List<Room> rooms = roomRepository.findAllByHotel_Id(hotelId);

        List<Long> bookedRoomIds = bookingRoomRepository.findBookedRoomIds(checkinDate, checkoutDate);

        return rooms.stream()
                .map(room -> {
                    RoomStatus status = room.getStatus();

                    if (status == RoomStatus.MAINTENANCE) {
                        room.setStatus(RoomStatus.MAINTENANCE);
                    } else if (bookedRoomIds.contains(room.getId())) {
                        room.setStatus(RoomStatus.BOOKED);
                    } else {
                        room.setStatus(RoomStatus.AVAILABLE);
                    }

                    return roomMapper.toResponse(room);
                })
                .toList();
    }
}
