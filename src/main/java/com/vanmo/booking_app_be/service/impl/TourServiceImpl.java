package com.vanmo.booking_app_be.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.vanmo.booking_app_be.dto.request.TourCreateRequest;
import com.vanmo.booking_app_be.dto.request.TourUpdateRequest;
import com.vanmo.booking_app_be.dto.response.TourResponse;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Tour;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.TourMapper;
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
public class TourServiceImpl implements TourService {

    private static final Comparator<Tour> LIST_ORDER = Comparator.comparing((Tour tour) -> !tour.isAvailable())
            .thenComparing(Tour::getPrice)
            .thenComparing(Tour::getName, String.CASE_INSENSITIVE_ORDER);

    private final TourRepository tourRepository;
    private final TourMapper tourMapper;
    private final FileStorageService fileStorageService;
    private final AccessGuard accessGuard;

    @Override
    public TourResponse create(TourCreateRequest req) {
        Hotel hotel = accessGuard.requireBranchManagerEditor(req.getHotelId());
        Tour tour = tourMapper.toEntity(req);
        tour.setName(req.getName().strip());
        tour.setHotel(hotel);
        blankToNull(tour);
        return tourMapper.toResponse(tourRepository.save(tour));
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
        return tourMapper.toResponse(tourRepository.save(tour));
    }

    @Override
    public TourResponse uploadImage(Long id, MultipartFile file) {
        Tour tour = findTour(id);
        accessGuard.requireBranchManagerEditor(tour.getHotel().getId());
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "file: must not be empty");
        }
        tour.setPathImage(fileStorageService.uploadHotelImage(file));
        return tourMapper.toResponse(tourRepository.save(tour));
    }

    @Override
    public TourResponse getById(Long id) {
        return tourMapper.toResponse(findTour(id));
    }

    @Override
    public List<TourResponse> getAllByHotel(Long hotelId) {
        return tourRepository.findAllByHotel_Id(hotelId).stream()
                .sorted(LIST_ORDER)
                .map(tourMapper::toResponse)
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
