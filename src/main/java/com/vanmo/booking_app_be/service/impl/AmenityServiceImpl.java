package com.vanmo.booking_app_be.service.impl;

import com.vanmo.booking_app_be.dto.request.AmenityCreateRequest;
import com.vanmo.booking_app_be.dto.request.AmenityUpdateRequest;
import com.vanmo.booking_app_be.dto.request.RoomAmenityCreateRequest;
import com.vanmo.booking_app_be.dto.response.AmenityResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.Amenity;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Room;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.AmenityMapper;
import com.vanmo.booking_app_be.repository.AmenityRepository;
import com.vanmo.booking_app_be.repository.HotelRepository;
import com.vanmo.booking_app_be.repository.RoomRepository;
import com.vanmo.booking_app_be.service.AmenityService;
import com.vanmo.booking_app_be.service.RoomAmenityService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AmenityServiceImpl implements AmenityService {

    private final AmenityRepository amenityRepository;
    private final HotelRepository hotelRepository;
    private final AmenityMapper amenityMapper;
    private final RoomRepository roomRepository;
    private final RoomAmenityService roomAmenityService;

    @Override
    @Transactional
    public AmenityResponse create(AmenityCreateRequest req) {
        Amenity entity = amenityMapper.toEntity(req);
        Hotel hotel = new Hotel();
        if (req.getHotelId() != null) {
             hotel = hotelRepository.findById(req.getHotelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Hotel not found id=" + req.getHotelId()));
            entity.setHotel(hotel);
        } else{
            throw new ResourceNotFoundException("Id hotel not found");
        }
        if (!req.isCommon() && req.getRoomId() != null) {
            Room room = roomRepository.findById(req.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Hotel not found id=" + req.getHotelId()));
            entity.setRoom(room);
            amenityRepository.save(entity);

            RoomAmenityCreateRequest roomAmenityCreateRequest = new RoomAmenityCreateRequest();
            roomAmenityCreateRequest.setQuantity(1);
            roomAmenityCreateRequest.setRoom(room);
            roomAmenityCreateRequest.setAmenity(entity);

            roomAmenityService.create(roomAmenityCreateRequest);

            AmenityResponse amenityResponse = amenityMapper.toResponse(entity);
            amenityResponse.setHotelName(hotel.getName());
            amenityResponse.setRoomName(room.getRoomNumber());

            return amenityResponse;
        }

        amenityRepository.save(entity);
        AmenityResponse amenityResponse = amenityMapper.toResponse(entity);
        amenityResponse.setHotelName(hotel.getName());

        return amenityResponse;
    }

    @Override
    public AmenityResponse update(Long id, AmenityUpdateRequest req) {
        Amenity entity = amenityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity not found id=" + id));
        amenityMapper.updateEntity(entity, req);
        amenityRepository.save(entity);
        AmenityResponse amenityResponse = amenityMapper.toResponse(entity);
        return amenityResponse;
    }

    @Override
    public AmenityResponse getById(Long id) {
        Amenity entity = amenityRepository.findById(id)
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
        List<Amenity> amenities = amenityRepository.getAllAmenityByRoom(roomId, hotelId);
        return amenities.stream().map(amenityMapper::toResponse).toList();
    }

    @Override
    public void delete(Long id) {
        Amenity entity = amenityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity not found id=" + id));
        amenityRepository.delete(entity);
    }
}
