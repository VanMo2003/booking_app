package com.vanmo.booking_app_be.service.impl;

import com.vanmo.booking_app_be.dto.request.RoomCreateRequest;
import com.vanmo.booking_app_be.dto.request.RoomUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.RoomResponse;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Room;
import com.vanmo.booking_app_be.entity.RoomType;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.RoomMapper;
import com.vanmo.booking_app_be.repository.HotelRepository;
import com.vanmo.booking_app_be.repository.RoomRepository;
import com.vanmo.booking_app_be.repository.RoomTypeRepository;
import com.vanmo.booking_app_be.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomMapper roomMapper;

    @Override
    public RoomResponse create(RoomCreateRequest req) {
        Hotel hotel = hotelRepository.findById(req.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found id=" + req.getHotelId()));

        boolean checkRoomNumber = roomRepository.existsByHotelAndRoomNumber(hotel, req.getRoomNumber());
        if (checkRoomNumber) {
            throw new ResourceNotFoundException("Room number already exists");
        }

        RoomType roomType = roomTypeRepository.findById(req.getRoomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("RoomType not found id=" + req.getRoomTypeId()));

        Room room = roomMapper.toEntity(req);
        room.setHotel(hotel);
        room.setRoomType(roomType);

        return roomMapper.toResponse(roomRepository.save(room));
    }

    @Override
    public RoomResponse update(Long id, RoomUpdateRequest req) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found id=" + id));

        RoomType roomType = roomTypeRepository.findById(req.getRoomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("RoomType not found id=" + req.getRoomTypeId()));

        roomMapper.updateEntity(room, req);
        room.setRoomType(roomType);

        return roomMapper.toResponse(roomRepository.save(room));
    }

    @Override
    public RoomResponse getById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found id=" + id));
        return roomMapper.toResponse(room);
    }

    @Override
    public PageResponse<RoomResponse> getAllByHotel(Long hotelId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("onCreate"));
        Page<RoomResponse> data = roomRepository.findByHotelId(hotelId, pageable).map(roomMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found id=" + id));
        roomRepository.delete(room);
    }
}
