package com.vanmo.booking_app_be.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vanmo.booking_app_be.dto.request.TourBookingCreateRequest;
import com.vanmo.booking_app_be.dto.response.TourBookingResponse;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Conversation;
import com.vanmo.booking_app_be.entity.Customer;
import com.vanmo.booking_app_be.entity.Tour;
import com.vanmo.booking_app_be.entity.TourBooking;
import com.vanmo.booking_app_be.entity.enums.ChatSide;
import com.vanmo.booking_app_be.entity.enums.NotificationType;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.entity.enums.TourBookingStatus;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.TourBookingMapper;
import com.vanmo.booking_app_be.repository.ConversationRepository;
import com.vanmo.booking_app_be.repository.CustomerRepository;
import com.vanmo.booking_app_be.repository.TourBookingRepository;
import com.vanmo.booking_app_be.repository.TourRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.NotificationService;
import com.vanmo.booking_app_be.service.TourBookingService;
import com.vanmo.booking_app_be.utils.AppLinks;
import com.vanmo.booking_app_be.utils.NotificationMessages;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TourBookingServiceImpl implements TourBookingService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Set<TourBookingStatus> OPEN =
            EnumSet.of(TourBookingStatus.PENDING, TourBookingStatus.CONFIRMED);
    private static final int MAX_DAYS_AHEAD = 365;
    private static final int MAX_GUESTS_PER_ORDER = 50;
    private static final int ASSISTANT_HISTORY = 5;

    private final TourBookingRepository tourBookingRepository;
    private final TourRepository tourRepository;
    private final CustomerRepository customerRepository;
    private final ConversationRepository conversationRepository;
    private final TourBookingMapper tourBookingMapper;
    private final NotificationService notificationService;
    private final AccessGuard accessGuard;

    @Override
    public TourBookingResponse create(TourBookingCreateRequest req) {
        Customer customer = currentCustomer();
        Tour tour = tourRepository.findById(req.getTourId()).orElseThrow(() -> invalid("Không tìm thấy tour này."));
        return place(tour, customer, null, req.getTourDate(), req.getGuests(), req.getNote(), false)
                .booking();
    }

    @Override
    public Placed placeFromChat(Long conversationId, Long tourId, LocalDate tourDate, int guests, String note) {
        Conversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found id=" + conversationId));
        Long hotelId = conversation.getHotel().getId();
        Tour tour = tourRepository
                .findById(tourId)
                .filter(t -> t.getHotel().getId().equals(hotelId))
                .orElseThrow(() -> invalid("Cơ sở không có tour mã " + tourId + "."));
        return place(tour, conversation.getCustomer(), conversation, tourDate, guests, note, true);
    }

    private Placed place(
            Tour tour,
            Customer customer,
            Conversation conversation,
            LocalDate tourDate,
            int guests,
            String note,
            boolean byAi) {
        LocalDate today = LocalDate.now(ZONE);
        if (!tour.isAvailable()) {
            throw invalid("Tour \"" + tour.getName() + "\" đang tạm ngừng nhận khách.");
        }
        if (tourDate.isBefore(today)) {
            throw invalid("Ngày đi " + tourDate.format(DAY) + " đã qua.");
        }
        if (tourDate.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw invalid("Chỉ nhận đặt tour trong vòng 1 năm tới.");
        }
        if (guests < 1 || guests > MAX_GUESTS_PER_ORDER) {
            throw invalid("Số khách mỗi đơn từ 1 đến " + MAX_GUESTS_PER_ORDER + ".");
        }

        // Same guest, tour and day: the order is already there (the guest said "đồng ý" twice).
        TourBooking existing = tourBookingRepository
                .findFirstByCustomer_IdAndTour_IdAndTourDateAndStatusIn(customer.getId(), tour.getId(), tourDate, OPEN)
                .orElse(null);
        if (existing != null) {
            if (existing.getGuests() == guests) {
                return new Placed(toResponse(existing), false);
            }
            throw invalid("Khách đã có đơn tour #" + existing.getId() + " cho tour này ngày " + tourDate.format(DAY)
                    + " (" + existing.getGuests() + " khách). Muốn đổi số khách thì huỷ đơn cũ trong mục Đơn tour"
                    + " rồi đặt lại.");
        }

        // maxGuests is the capacity of one departure, shared by every open order that day.
        if (tour.getMaxGuests() != null) {
            int taken = tourBookingRepository.sumGuests(tour.getId(), tourDate, OPEN);
            int left = tour.getMaxGuests() - taken;
            if (guests > left) {
                throw invalid(
                        left <= 0
                                ? "Tour \"" + tour.getName() + "\" ngày " + tourDate.format(DAY) + " đã kín chỗ."
                                : "Tour \"" + tour.getName() + "\" ngày " + tourDate.format(DAY) + " chỉ còn " + left
                                        + " chỗ.");
            }
        }

        TourBooking booking = tourBookingRepository.save(TourBooking.builder()
                .tour(tour)
                .tourName(tour.getName())
                .hotel(tour.getHotel())
                .customer(customer)
                .conversation(conversation)
                .tourDate(tourDate)
                .guests(guests)
                .unitPrice(tour.getPrice())
                .totalAmount(tour.getPrice().multiply(BigDecimal.valueOf(guests)))
                .status(TourBookingStatus.PENDING)
                .note(trimmed(note, 500))
                .createdByAi(byAi)
                .build());
        notificationService.notifyAccounts(
                accessGuard.branchTeam(booking.getHotel()),
                NotificationType.TOUR_BOOKING_CREATED,
                NotificationMessages.tourBookingCreated(booking),
                AppLinks.tourBooking(booking.getId()),
                booking.getId());
        return new Placed(toResponse(booking), true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourBookingResponse> getMine() {
        Account caller = accessGuard.currentAccount();
        if (caller.getRole() != Role.CUSTOMER) {
            throw new AppException(ErrorCode.UN_AUTHORIZED);
        }
        Customer customer = customerRepository.findByAccount_Id(caller.getId());
        if (customer == null) {
            return List.of();
        }
        return tourBookingRepository.findAllByCustomer_IdOrderByIdDesc(customer.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourBookingResponse> getByHotel(Long hotelId, TourBookingStatus status) {
        accessGuard.requireBranchStaffOrAbove(hotelId);
        List<TourBooking> bookings = status == null
                ? tourBookingRepository.findAllByHotel_IdOrderByIdDesc(hotelId)
                : tourBookingRepository.findAllByHotel_IdAndStatusOrderByIdDesc(hotelId, status);
        return bookings.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TourBookingResponse getById(Long id) {
        TourBooking booking = find(id);
        Account caller = accessGuard.currentAccount();
        if (caller.getRole() == Role.CUSTOMER) {
            requireOwner(booking, caller);
        } else {
            accessGuard.requireBranchStaffOrAbove(booking.getHotel().getId());
        }
        return toResponse(booking);
    }

    @Override
    public TourBookingResponse confirm(Long id) {
        TourBooking booking = find(id);
        accessGuard.requireBranchEditor(booking.getHotel().getId());
        if (booking.getStatus() != TourBookingStatus.PENDING) {
            throw invalid("Chỉ xác nhận được đơn tour đang chờ xác nhận.");
        }
        booking.setStatus(TourBookingStatus.CONFIRMED);
        booking.setHandledBy(accessGuard.currentAccount());
        tourBookingRepository.save(booking);
        notifyGuest(
                booking, NotificationType.TOUR_BOOKING_CONFIRMED, NotificationMessages.tourBookingConfirmed(booking));
        return toResponse(booking);
    }

    @Override
    public TourBookingResponse complete(Long id) {
        TourBooking booking = find(id);
        accessGuard.requireBranchEditor(booking.getHotel().getId());
        if (booking.getStatus() != TourBookingStatus.CONFIRMED) {
            throw invalid("Chỉ hoàn tất được đơn tour đã xác nhận.");
        }
        booking.setStatus(TourBookingStatus.COMPLETED);
        booking.setHandledBy(accessGuard.currentAccount());
        return toResponse(tourBookingRepository.save(booking));
    }

    @Override
    public TourBookingResponse cancel(Long id, String reason) {
        TourBooking booking = find(id);
        Account caller = accessGuard.currentAccount();
        boolean byGuest = caller.getRole() == Role.CUSTOMER;
        if (byGuest) {
            requireOwner(booking, caller);
        } else {
            accessGuard.requireBranchEditor(booking.getHotel().getId());
        }
        if (!OPEN.contains(booking.getStatus())) {
            throw invalid("Đơn tour đã hoàn tất hoặc đã huỷ.");
        }
        String why = trimmed(reason, 255);
        if (!byGuest && why == null) {
            throw invalid("Hãy nhập lý do huỷ để báo cho khách.");
        }
        booking.setStatus(TourBookingStatus.CANCELED);
        booking.setCancelReason(why);
        booking.setCanceledBy(byGuest ? ChatSide.CUSTOMER : ChatSide.HOTEL);
        if (!byGuest) {
            booking.setHandledBy(caller);
        }
        tourBookingRepository.save(booking);
        if (byGuest) {
            notificationService.notifyAccounts(
                    accessGuard.branchTeam(booking.getHotel()),
                    NotificationType.TOUR_BOOKING_CANCELED,
                    NotificationMessages.tourBookingCanceledByCustomer(booking),
                    AppLinks.tourBooking(booking.getId()),
                    booking.getId());
        } else {
            notifyGuest(
                    booking,
                    NotificationType.TOUR_BOOKING_CANCELED,
                    NotificationMessages.tourBookingCanceledByHotel(booking));
        }
        return toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourBookingResponse> recentForAssistant(Long customerId, Long hotelId) {
        return tourBookingRepository
                .findByCustomer_IdAndHotel_IdOrderByIdDesc(customerId, hotelId, PageRequest.of(0, ASSISTANT_HISTORY))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void notifyGuest(TourBooking booking, NotificationType type, NotificationMessages.Message message) {
        // Walk-in profiles have no account to notify.
        Account guest = booking.getCustomer().getAccount();
        if (guest != null) {
            notificationService.notify(guest, type, message, AppLinks.tourBooking(booking.getId()), booking.getId());
        }
    }

    private Customer currentCustomer() {
        Account caller = accessGuard.currentAccount();
        if (caller.getRole() != Role.CUSTOMER) {
            throw new AppException(ErrorCode.UN_AUTHORIZED, "Only customers can book tours");
        }
        Customer customer = customerRepository.findByAccount_Id(caller.getId());
        if (customer == null) {
            throw new AppException(
                    ErrorCode.CUSTOMER_PROFILE_REQUIRED, "Create your customer profile before booking a tour");
        }
        return customer;
    }

    private static void requireOwner(TourBooking booking, Account caller) {
        Account owner = booking.getCustomer().getAccount();
        if (owner == null || !owner.getId().equals(caller.getId())) {
            throw new AppException(ErrorCode.UN_AUTHORIZED);
        }
    }

    private TourBooking find(Long id) {
        return tourBookingRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour booking not found id=" + id));
    }

    private TourBookingResponse toResponse(TourBooking booking) {
        return tourBookingMapper.toResponse(booking);
    }

    private static AppException invalid(String message) {
        return new AppException(ErrorCode.TOUR_BOOKING_INVALID, message);
    }

    private static String trimmed(String text, int max) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.strip();
        return value.length() <= max ? value : value.substring(0, max);
    }
}
