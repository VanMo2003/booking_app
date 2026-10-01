package com.vanmo.booking_app_be.service.impl;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.vanmo.booking_app_be.dto.request.TourCreateRequest;
import com.vanmo.booking_app_be.dto.request.TourUpdateRequest;
import com.vanmo.booking_app_be.dto.response.TourResponse;
import com.vanmo.booking_app_be.dto.response.TourRoomResponse;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Room;
import com.vanmo.booking_app_be.entity.Tour;
import com.vanmo.booking_app_be.entity.TourRoom;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.TourMapper;
import com.vanmo.booking_app_be.repository.RoomRepository;
import com.vanmo.booking_app_be.repository.TourRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.FileStorageService;
import com.vanmo.booking_app_be.service.TourService;

import lombok.RequiredArgsConstructor;

/**
 * Tours a branch offers. Anyone may read them (public browsing, and the chat assistant suggests them); only the
 * branch's HOTEL_OWNER or HOTEL_MANAGER writes them - staff and admins are read-only
 * ({@link AccessGuard#requireBranchManagerEditor(Long)}), same rule as the food menu.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TourServiceImpl implements TourService {

    private static final Comparator<Tour> LIST_ORDER = Comparator.comparing((Tour tour) -> !tour.isAvailable())
            .thenComparing(Tour::getPrice)
            .thenComparing(Tour::getName, String.CASE_INSENSITIVE_ORDER);

    private final TourRepository tourRepository;
    private final RoomRepository roomRepository;
    private final TourMapper tourMapper;
    private final FileStorageService fileStorageService;
    private final AccessGuard accessGuard;

    @Override
    public TourResponse create(TourCreateRequest req) {
        Hotel hotel = accessGuard.requireBranchManagerEditor(req.getHotelId());
        Tour tour = tourMapper.toEntity(req);
        tour.setName(req.getName().strip());
        tour.setHotel(hotel);
        tour.setStayNights(req.getStayNights() == null ? 1 : req.getStayNights());
        blankToNull(tour);
        if (req.getRoomIds() != null) {
            setRooms(tour, req.getRoomIds());
        }
        return toResponse(tourRepository.save(tour));
    }

    @Override
    public TourResponse update(Long id, TourUpdateRequest req) {
        Tour tour = findTour(id);
        accessGuard.requireBranchManagerEditor(tour.getHotel().getId());
        if (req.getName() != null && req.getName().isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "name: must not be blank");
        }
        tourMapper.updateEntity(tour, req);
        if (req.getName() != null) {
            tour.setName(req.getName().strip());
        }
        if (req.getMaxGuests() != null && req.getMaxGuests() == 0) {
            tour.setMaxGuests(null);
        }
        blankToNull(tour);
        if (req.getRoomIds() != null) {
            setRooms(tour, req.getRoomIds());
        }
        return toResponse(tourRepository.save(tour));
    }

    /**
     * Makes the package use exactly these rooms of the tour's branch. Only the difference is written: re-adding a
     * pair that is being removed in the same flush would hit the unique key (Hibernate inserts before it deletes).
     */
    private void setRooms(Tour tour, Set<Long> roomIds) {
        Long hotelId = tour.getHotel().getId();
        if (tour.getTourRooms() == null) {
            tour.setTourRooms(new HashSet<>());
        }
        List<Room> rooms = roomRepository.findAllById(roomIds);
        if (rooms.size() != roomIds.size()
                || rooms.stream().anyMatch(room -> !room.getHotel().getId().equals(hotelId))) {
            throw new AppException(ErrorCode.BAD_REQUEST, "roomIds: every room must belong to this branch");
        }
        tour.getTourRooms().removeIf(link -> !roomIds.contains(link.getRoom().getId()));
        Set<Long> kept =
                tour.getTourRooms().stream().map(link -> link.getRoom().getId()).collect(Collectors.toSet());
        rooms.stream().filter(room -> !kept.contains(room.getId())).forEach(room -> tour.getTourRooms()
                .add(TourRoom.builder().tour(tour).room(room).build()));
    }

    /** Rooms of a tour package, cheapest first, as every tour reader shows them. */
    public static List<TourRoomResponse> roomsOf(Collection<TourRoom> links) {
        if (links == null) {
            return List.of();
        }
        return links.stream()
                .map(TourRoom::getRoom)
                .sorted(Comparator.comparing(Room::getPrice).thenComparing(Room::getRoomNumber))
                .map(TourServiceImpl::roomResponse)
                .toList();
    }

    public static TourRoomResponse roomResponse(Room room) {
        return TourRoomResponse.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .roomTypeName(room.getRoomType().getName())
                .capacity(room.getCapacity())
                .price(room.getPrice())
                .pathImage(room.getPathImage())
                .build();
    }

    private TourResponse toResponse(Tour tour) {
        TourResponse response = tourMapper.toResponse(tour);
        if (response.getStayNights() == null) {
            response.setStayNights(1);
        }
        response.setRooms(roomsOf(tour.getTourRooms()));
        return response;
    }

    @Override
    public TourResponse uploadImage(Long id, MultipartFile file) {
        Tour tour = findTour(id);
        accessGuard.requireBranchManagerEditor(tour.getHotel().getId());
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "file: must not be empty");
        }
        tour.setPathImage(fileStorageService.uploadHotelImage(file));
        return toResponse(tourRepository.save(tour));
    }

    @Override
    public TourResponse getById(Long id) {
        return toResponse(findTour(id));
    }

    @Override
    public List<TourResponse> getAllByHotel(Long hotelId) {
        return tourRepository.findAllByHotel_Id(hotelId).stream()
                .sorted(LIST_ORDER)
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void delete(Long id) {
        Tour tour = findTour(id);
        accessGuard.requireBranchManagerEditor(tour.getHotel().getId());
        tourRepository.delete(tour);
    }

    /** Empty optional texts are stored as null so the app and the assistant simply leave them out. */
    private static void blankToNull(Tour tour) {
        clear(tour.getDescription(), tour::setDescription);
        clear(tour.getDuration(), tour::setDuration);
        clear(tour.getDeparture(), tour::setDeparture);
        clear(tour.getIncludes(), tour::setIncludes);
        clear(tour.getPathImage(), tour::setPathImage);
    }

    private static void clear(String value, Consumer<String> setter) {
        if (value != null && value.isBlank()) {
            setter.accept(null);
        } else if (value != null) {
            setter.accept(value.strip());
        }
    }

    private Tour findTour(Long id) {
        return tourRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tour not found id=" + id));
    }
}
