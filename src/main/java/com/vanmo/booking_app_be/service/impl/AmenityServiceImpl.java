package com.vanmo.booking_app_be.service.impl;

import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.vanmo.booking_app_be.dto.request.AmenityCreateRequest;
import com.vanmo.booking_app_be.dto.request.AmenityUpdateRequest;
import com.vanmo.booking_app_be.dto.request.RoomAmenityCreateRequest;
import com.vanmo.booking_app_be.dto.response.AmenityResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.Amenity;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Room;
import com.vanmo.booking_app_be.entity.RoomAmenity;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.AmenityMapper;
import com.vanmo.booking_app_be.repository.AmenityRepository;
import com.vanmo.booking_app_be.repository.HotelRepository;
import com.vanmo.booking_app_be.repository.RoomAmenityRepository;
import com.vanmo.booking_app_be.repository.RoomRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.AmenityService;
import com.vanmo.booking_app_be.service.RoomAmenityService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AmenityServiceImpl implements AmenityService {
    private final AmenityRepository amenityRepository;
    private final RoomAmenityRepository roomAmenityRepository;
    private final HotelRepository hotelRepository;
    private final AmenityMapper amenityMapper;
    private final RoomRepository roomRepository;
    private final RoomAmenityService roomAmenityService;
    private final AccessGuard accessGuard;

    @Override
    @Transactional
    public AmenityResponse create(AmenityCreateRequest req) {
        if (req.getHotelId() == null) {
            throw new ResourceNotFoundException("Id hotel not found");
        }
        Amenity amenity = amenityRepository.findByHotelIdAndName(req.getHotelId(), req.getName());
        if (amenity != null) throw new ResourceNotFoundException("Amenity with this name already exists");
        amenity = amenityMapper.toEntity(req);
        Hotel hotel = accessGuard.requireBranchStaffOrAbove(req.getHotelId());
        amenity.setHotel(hotel);
        if (!req.isCommon() && req.getRoomId() != null) {
            Room room = roomRepository
                    .findById(req.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
            amenityRepository.save(amenity);

            RoomAmenityCreateRequest roomAmenityCreateRequest = new RoomAmenityCreateRequest();
            roomAmenityCreateRequest.setQuantity(1);
            roomAmenityCreateRequest.setRoomId(room.getId());
            roomAmenityCreateRequest.setAmenityId(amenity.getId());

            roomAmenityService.create(roomAmenityCreateRequest);

            AmenityResponse amenityResponse = amenityMapper.toResponse(amenity);
            amenityResponse.setHotelName(hotel.getName());
            amenityResponse.setRoomName(room.getRoomNumber());

            return amenityResponse;
        }

        amenityRepository.save(amenity);
        AmenityResponse amenityResponse = amenityMapper.toResponse(amenity);
        amenityResponse.setHotelName(hotel.getName());

        return amenityResponse;
    }

    @Override
    public AmenityResponse update(Long id, AmenityUpdateRequest req) {
        Amenity entity = amenityRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity not found id=" + id));
        accessGuard.requireBranchStaffOrAbove(entity.getHotel().getId());
        amenityMapper.updateEntity(entity, req);
        amenityRepository.save(entity);
        return amenityMapper.toResponse(entity);
    }

    @Override
    public AmenityResponse getById(Long id) {
        Amenity entity = amenityRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity not found id=" + id));
        return amenityMapper.toResponse(entity);
    }

    @Override
    public PageResponse<AmenityResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id"));
        Page<AmenityResponse> data = amenityRepository.findAll(pageable).map(amenityMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public List<AmenityResponse> getAllByHotel(Long hotelId) {
        List<Amenity> amenities = amenityRepository.findByHotelIdAndCommon(hotelId, true);
        return amenities.stream().map(amenityMapper::toResponse).toList();
    }

    @Override
    public List<AmenityResponse> getAllByRoom(Long hotelId, Long roomId) {
        List<Amenity> amenities = roomAmenityRepository.findAllByRoomId(roomId).stream()
                .map(RoomAmenity::getAmenity)
                .toList();
        return amenities.stream().map(amenityMapper::toResponse).toList();
    }

    @Override
    public void delete(Long id) {
        Amenity entity = amenityRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity not found id=" + id));
        accessGuard.requireBranchManagerOrAbove(entity.getHotel().getId());
        if (!entity.isCommon()) {
            List<RoomAmenity> roomAmenities = roomAmenityRepository.findAllByAmenity(entity);
            roomAmenityRepository.deleteAll(roomAmenities);
        }
        amenityRepository.delete(entity);
    }
}
