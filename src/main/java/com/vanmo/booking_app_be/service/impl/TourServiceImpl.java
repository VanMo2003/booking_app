package com.vanmo.booking_app_be.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.vanmo.booking_app_be.dto.request.ItineraryStepRequest;
import com.vanmo.booking_app_be.dto.request.TourCreateRequest;
import com.vanmo.booking_app_be.dto.request.TourUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.TourDestinationDTO;
import com.vanmo.booking_app_be.dto.response.TourHomeResponse;
import com.vanmo.booking_app_be.dto.response.TourResponse;
import com.vanmo.booking_app_be.dto.response.TourRoomResponse;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.ItineraryStep;
import com.vanmo.booking_app_be.entity.Room;
import com.vanmo.booking_app_be.entity.Tour;
import com.vanmo.booking_app_be.entity.TourRoom;
import com.vanmo.booking_app_be.entity.enums.TravelTheme;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.TourMapper;
import com.vanmo.booking_app_be.repository.CustomerRepository;
import com.vanmo.booking_app_be.repository.RoomRepository;
import com.vanmo.booking_app_be.repository.TourRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.FileStorageService;
import com.vanmo.booking_app_be.service.NotificationService;
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
    private final CustomerRepository customerRepository;
    private final NotificationService notificationService;

    @Override
    public TourResponse create(TourCreateRequest req) {
        Hotel hotel = accessGuard.requireBranchManagerEditor(req.getHotelId());
        Tour tour = tourMapper.toEntity(req);
        tour.setName(req.getName().strip());
        tour.setHotel(hotel);
        tour.setStayNights(req.getStayNights() == null ? 1 : req.getStayNights());
        applyDetails(tour, req.getThemes(), req.getItinerary(), req.getFeatured());
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
        boolean wasOnSale = TourPricing.onSale(tour, TourPricing.today());
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
        applyDetails(tour, req.getThemes(), req.getItinerary(), req.getFeatured());
        blankToNull(tour);
        if (req.getRoomIds() != null) {
            setRooms(tour, req.getRoomIds());
        }
        Tour saved = tourRepository.save(tour);
        if (!wasOnSale && saved.isAvailable() && TourPricing.onSale(saved, TourPricing.today())) {
            announceSale(saved);
        }
        return toResponse(saved);
    }

    /** Guests whose interests match the tour hear that it just went on sale. */
    private void announceSale(Tour tour) {
        if (tour.getThemes() == null || tour.getThemes().isEmpty()) {
            return;
        }
        List<Account> guests = customerRepository.findAccountsInterestedIn(tour.getThemes());
        if (!guests.isEmpty()) {
            notificationService.notifyAccounts(
                    guests,
                    com.vanmo.booking_app_be.entity.enums.NotificationType.PROMOTION,
                    com.vanmo.booking_app_be.utils.NotificationMessages.tourOnSale(
                            tour.getName(), tour.getDestination(), tour.getDiscountPercent()),
                    com.vanmo.booking_app_be.utils.AppLinks.tour(tour.getId()),
                    tour.getId());
        }
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

    /** Themes, programme and the featured flag (null = leave as is); a 0 % discount switches the promotion off. */
    private static void applyDetails(
            Tour tour, Set<TravelTheme> themes, List<ItineraryStepRequest> itinerary, Boolean featured) {
        if (themes != null) {
            tour.getThemes().clear();
            tour.getThemes().addAll(themes);
        }
        if (itinerary != null) {
            tour.getItinerary().clear();
            for (ItineraryStepRequest step : itinerary) {
                tour.getItinerary()
                        .add(ItineraryStep.builder()
                                .time(blankToNull(step.getTime()))
                                .title(step.getTitle().strip())
                                .description(blankToNull(step.getDescription()))
                                .build());
            }
        }
        if (featured != null) {
            tour.setFeatured(featured);
        }
        if (tour.getDiscountPercent() != null && tour.getDiscountPercent() == 0) {
            tour.setDiscountPercent(null);
            tour.setPromotionEndsAt(null);
        }
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
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

    TourResponse toResponse(Tour tour) {
        TourResponse response = tourMapper.toResponse(tour);
        if (response.getStayNights() == null) {
            response.setStayNights(1);
        }
        response.setRooms(roomsOf(tour.getTourRooms()));
        response.setItinerary(
                tour.getItinerary() == null
                        ? List.of()
                        : tour.getItinerary().stream()
                                .map(step -> new ItineraryStepRequest(
                                        step.getTime(), step.getTitle(), step.getDescription()))
                                .toList());
        TourPricing.Price price = TourPricing.of(tour, TourPricing.today());
        response.setOnSale(price.onSale());
        response.setSalePrice(price.adult());
        response.setSaleChildPrice(price.child());
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

    @Override
    public PageResponse<TourResponse> search(String q, TravelTheme theme, BigDecimal maxPrice, int page, int size) {
        String text = q == null || q.isBlank() ? null : q.strip();
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Order.desc("featured"), Sort.Order.desc("id")));
        return PageResponse.from(
                tourRepository.searchPublic(text, theme, maxPrice, pageable).map(this::toResponse));
    }

    @Override
    public List<TourResponse> publicTours() {
        return tourRepository.findAllPublic().stream()
                .sorted(LIST_ORDER)
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TourDestinationDTO> destinations() {
        return tourRepository.findDestinations();
    }

    @Override
    public TourHomeResponse home(Set<TravelTheme> interests) {
        List<Tour> tours = tourRepository.findAllPublic();
        LocalDate today = TourPricing.today();
        return TourHomeResponse.builder()
                .featured(TourDiscovery.featured(tours).stream()
                        .map(this::toResponse)
                        .toList())
                .deals(TourDiscovery.deals(tours, today).stream()
                        .map(this::toResponse)
                        .toList())
                .forYou(TourDiscovery.forYou(tours, interests, today).stream()
                        .map(this::toResponse)
                        .toList())
                .destinations(tourRepository.findDestinations())
                .build();
    }

    /** Empty optional texts are stored as null so the app and the assistant simply leave them out. */
    private static void blankToNull(Tour tour) {
        clear(tour.getDescription(), tour::setDescription);
        clear(tour.getDuration(), tour::setDuration);
        clear(tour.getDeparture(), tour::setDeparture);
        clear(tour.getIncludes(), tour::setIncludes);
        clear(tour.getPathImage(), tour::setPathImage);
        clear(tour.getDestination(), tour::setDestination);
        clear(tour.getCancellationPolicy(), tour::setCancellationPolicy);
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
