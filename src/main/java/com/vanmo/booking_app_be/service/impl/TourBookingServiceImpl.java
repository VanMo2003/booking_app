package com.vanmo.booking_app_be.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vanmo.booking_app_be.dto.request.TourBookingCreateRequest;
import com.vanmo.booking_app_be.dto.response.TourBookingResponse;
import com.vanmo.booking_app_be.dto.response.TourRoomAvailabilityResponse;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Booking;
import com.vanmo.booking_app_be.entity.BookingRoom;
import com.vanmo.booking_app_be.entity.Conversation;
import com.vanmo.booking_app_be.entity.Customer;
import com.vanmo.booking_app_be.entity.Room;
import com.vanmo.booking_app_be.entity.Tour;
import com.vanmo.booking_app_be.entity.TourBooking;
import com.vanmo.booking_app_be.entity.TourRoom;
import com.vanmo.booking_app_be.entity.Voucher;
import com.vanmo.booking_app_be.entity.enums.BookingStatus;
import com.vanmo.booking_app_be.entity.enums.ChatSide;
import com.vanmo.booking_app_be.entity.enums.NotificationType;
import com.vanmo.booking_app_be.entity.enums.PaymentMethod;
import com.vanmo.booking_app_be.entity.enums.PaymentStatus;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.entity.enums.RoomStatus;
import com.vanmo.booking_app_be.entity.enums.TourBookingStatus;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.TourBookingMapper;
import com.vanmo.booking_app_be.repository.BookingRepository;
import com.vanmo.booking_app_be.repository.BookingRoomRepository;
import com.vanmo.booking_app_be.repository.ConversationRepository;
import com.vanmo.booking_app_be.repository.CustomerRepository;
import com.vanmo.booking_app_be.repository.TourBookingRepository;
import com.vanmo.booking_app_be.repository.TourRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.NotificationService;
import com.vanmo.booking_app_be.service.TourBookingService;
import com.vanmo.booking_app_be.service.VoucherService;
import com.vanmo.booking_app_be.utils.AppLinks;
import com.vanmo.booking_app_be.utils.NotificationMessages;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TourBookingServiceImpl implements TourBookingService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Orders that still hold a seat; the AI tour search counts them too. */
    public static final Set<TourBookingStatus> OPEN =
            EnumSet.of(TourBookingStatus.PENDING, TourBookingStatus.CONFIRMED);

    private static final int MAX_DAYS_AHEAD = 365;
    private static final int MAX_GUESTS_PER_ORDER = 50;
    private static final int ASSISTANT_HISTORY = 5;
    private static final Set<BookingStatus> WAITING = EnumSet.of(BookingStatus.PENDING, BookingStatus.PAYING);
    private static final Comparator<Room> ROOM_ORDER =
            Comparator.comparing(Room::getPrice).thenComparing(Room::getRoomNumber);

    private final TourBookingRepository tourBookingRepository;
    private final BookingRepository bookingRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final TourRepository tourRepository;
    private final CustomerRepository customerRepository;
    private final ConversationRepository conversationRepository;
    private final TourBookingMapper tourBookingMapper;
    private final NotificationService notificationService;
    private final VoucherService voucherService;
    private final AccessGuard accessGuard;

    @Override
    public TourBookingResponse create(TourBookingCreateRequest req) {
        Customer customer = currentCustomer();
        Tour tour = tourRepository.findById(req.getTourId()).orElseThrow(() -> invalid("Không tìm thấy tour này."));
        return place(
                        tour,
                        customer,
                        null,
                        req.getTourDate(),
                        req.getGuests(),
                        req.getChildren() == null ? 0 : req.getChildren(),
                        req.getVoucherCode(),
                        req.getRoomIds(),
                        req.getNote(),
                        false)
                .booking();
    }

    @Override
    public Placed placeFromChat(
            Long conversationId, Long tourId, LocalDate tourDate, int guests, List<String> roomNumbers, String note) {
        Conversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found id=" + conversationId));
        Long hotelId = conversation.getHotel().getId();
        Tour tour = tourRepository
                .findById(tourId)
                .filter(t -> t.getHotel().getId().equals(hotelId))
                .orElseThrow(() -> invalid("Cơ sở không có tour mã " + tourId + "."));
        return place(
                tour,
                conversation.getCustomer(),
                conversation,
                tourDate,
                guests,
                0,
                null,
                roomIdsByNumber(tour, roomNumbers),
                note,
                true);
    }

    @Override
    @Transactional(readOnly = true)
    public TourRoomAvailabilityResponse availableRooms(Long tourId, LocalDate tourDate) {
        Tour tour = tourRepository
                .findById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found id=" + tourId));
        Stay stay = Stay.of(tour, tourDate);
        List<Room> packageRooms = packageRooms(tour);
        return TourRoomAvailabilityResponse.builder()
                .tourId(tour.getId())
                .hotelId(tour.getHotel().getId())
                .checkin(stay.checkin())
                .checkout(stay.checkout())
                .nights(stay.nights())
                .withRooms(!packageRooms.isEmpty())
                .rooms(freeRooms(packageRooms, stay).stream()
                        .sorted(ROOM_ORDER)
                        .map(TourServiceImpl::roomResponse)
                        .toList())
                .build();
    }

    /** The stay sold with a tour: check-in on the tour date, the tour's number of nights. */
    private record Stay(LocalDate checkin, LocalDate checkout, int nights) {
        static Stay of(Tour tour, LocalDate tourDate) {
            int nights = tour.getStayNights() == null ? 1 : Math.max(1, tour.getStayNights());
            return new Stay(tourDate, tourDate.plusDays(nights), nights);
        }
    }

    private Placed place(
            Tour tour,
            Customer customer,
            Conversation conversation,
            LocalDate tourDate,
            int guests,
            int children,
            String voucherCode,
            Collection<Long> roomIds,
            String note,
            boolean byAi) {
        Set<Long> chosen = roomIds == null ? Set.of() : new LinkedHashSet<>(roomIds);
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
        if (children < 0 || children >= guests) {
            throw invalid("Mỗi đơn cần ít nhất 1 người lớn.");
        }

        // Same guest, tour and day: the order is already there (the guest said "đồng ý" twice).
        TourBooking existing = tourBookingRepository
                .findFirstByCustomer_IdAndTour_IdAndTourDateAndStatusIn(customer.getId(), tour.getId(), tourDate, OPEN)
                .orElse(null);
        if (existing != null) {
            if (existing.getGuests() == guests && roomIdsOf(existing).equals(chosen)) {
                return new Placed(toResponse(existing), false);
            }
            throw invalid("Khách đã có đơn tour #" + existing.getId() + " cho tour này ngày " + tourDate.format(DAY)
                    + " (" + existing.getGuests() + " khách" + roomList(roomsOf(existing)) + "). Muốn đổi số khách"
                    + " hay phòng thì huỷ đơn cũ trong mục Đơn tour rồi đặt lại.");
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

        Stay stay = Stay.of(tour, tourDate);
        List<Room> rooms = chooseRooms(tour, stay, chosen, guests);
        TourPricing.Price price = TourPricing.of(tour, TourPricing.today());
        BigDecimal grossTourAmount = VoucherServiceImpl.tourAmount(tour, guests - children, children);
        Voucher voucher = voucherCode == null || voucherCode.isBlank()
                ? null
                : voucherService.reserve(voucherCode, customer, tour.getHotel(), grossTourAmount);
        BigDecimal voucherDiscount =
                voucher == null ? BigDecimal.ZERO : VoucherRules.discount(voucher, grossTourAmount);
        BigDecimal tourAmount = grossTourAmount.subtract(voucherDiscount);
        BigDecimal roomAmount = rooms.stream()
                .map(Room::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(BigDecimal.valueOf(stay.nights()));

        TourBooking booking = tourBookingRepository.save(TourBooking.builder()
                .tour(tour)
                .tourName(tour.getName())
                .hotel(tour.getHotel())
                .customer(customer)
                .conversation(conversation)
                .tourDate(tourDate)
                .guests(guests)
                .children(children)
                .unitPrice(price.adult())
                .childUnitPrice(children > 0 ? price.child() : null)
                .voucher(voucher)
                .voucherCode(voucher == null ? null : voucher.getCode())
                .voucherDiscount(voucher == null ? null : voucherDiscount)
                .tourAmount(tourAmount)
                .roomAmount(roomAmount)
                .totalAmount(tourAmount.add(roomAmount))
                .stayCheckout(rooms.isEmpty() ? null : stay.checkout())
                .status(TourBookingStatus.PENDING)
                .note(trimmed(note, 500))
                .createdByAi(byAi)
                .build());
        if (!rooms.isEmpty()) {
            booking.setBooking(bookRooms(booking, rooms, stay));
        }
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
        Booking stay = booking.getBooking();
        if (stay != null && WAITING.contains(stay.getBookingStatus())) {
            stay.setBookingStatus(BookingStatus.CONFIRMED);
        }
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
        voucherService.release(booking.getVoucher());
        booking.setCanceledBy(byGuest ? ChatSide.CUSTOMER : ChatSide.HOTEL);
        if (!byGuest) {
            booking.setHandledBy(caller);
        }
        // The rooms came with the tour: free them too, unless the guest already stayed (completed).
        Booking stay = booking.getBooking();
        if (stay != null
                && (WAITING.contains(stay.getBookingStatus()) || stay.getBookingStatus() == BookingStatus.CONFIRMED)) {
            stay.setBookingStatus(BookingStatus.CANCELED);
            stay.setCanceledAt(new Date());
            stay.setCanceledBy(booking.getCanceledBy());
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

    /**
     * Checks the guest's room choice against the package: the rooms must be the tour's, free for the whole stay (not
     * under maintenance, no overlapping pending/confirmed booking) and sleep everyone. A tour without rooms takes none.
     */
    private List<Room> chooseRooms(Tour tour, Stay stay, Set<Long> chosen, int guests) {
        List<Room> packageRooms = packageRooms(tour);
        if (packageRooms.isEmpty()) {
            if (!chosen.isEmpty()) {
                throw invalid("Tour \"" + tour.getName() + "\" không kèm phòng.");
            }
            return List.of();
        }
        if (chosen.isEmpty()) {
            throw invalid("Hãy chọn phòng cho tour \"" + tour.getName() + "\" (phòng của gói: "
                    + packageRooms.stream()
                            .sorted(ROOM_ORDER)
                            .map(Room::getRoomNumber)
                            .collect(Collectors.joining(", "))
                    + ").");
        }
        Map<Long, Room> byId = packageRooms.stream().collect(Collectors.toMap(Room::getId, room -> room));
        Set<Long> free = freeRooms(packageRooms, stay).stream().map(Room::getId).collect(Collectors.toSet());
        List<Room> rooms = new ArrayList<>();
        for (Long id : chosen) {
            Room room = byId.get(id);
            if (room == null) {
                throw invalid("Phòng mã " + id + " không thuộc gói tour \"" + tour.getName() + "\".");
            }
            if (!free.contains(id)) {
                throw invalid("Phòng " + room.getRoomNumber() + " đã có khách từ "
                        + stay.checkin().format(DAY) + " đến " + stay.checkout().format(DAY)
                        + ". Hãy chọn phòng khác.");
            }
            rooms.add(room);
        }
        int beds = rooms.stream().mapToInt(Room::getCapacity).sum();
        if (beds < guests) {
            throw invalid("Các phòng đã chọn chỉ đủ cho " + beds + " khách, đơn có " + guests
                    + " khách. Hãy chọn thêm phòng.");
        }
        return rooms;
    }

    /** The room booking that goes with a tour order: a normal booking, paid at the hotel unless the guest switches. */
    private Booking bookRooms(TourBooking tourBooking, List<Room> rooms, Stay stay) {
        Booking booking = Booking.builder()
                .checkinDate(stay.checkin())
                .checkoutDate(stay.checkout())
                .bookingStatus(BookingStatus.PENDING)
                .paymentMethod(PaymentMethod.CASH)
                .paymentStatus(PaymentStatus.UNPAID)
                .hotel(tourBooking.getHotel())
                .customer(tourBooking.getCustomer())
                .totalAmount(tourBooking.getRoomAmount())
                .note("Phòng kèm đơn tour #" + tourBooking.getId() + ": " + tourBooking.getTourName() + " ngày "
                        + tourBooking.getTourDate().format(DAY) + ", " + tourBooking.getGuests() + " khách.")
                .bookingServices(new HashSet<>())
                .build();
        booking.setBookingRooms(rooms.stream()
                .map(room -> BookingRoom.builder().booking(booking).room(room).build())
                .collect(Collectors.toCollection(HashSet::new)));
        return bookingRepository.save(booking);
    }

    private List<Room> freeRooms(List<Room> packageRooms, Stay stay) {
        Set<Long> booked = new HashSet<>(bookingRoomRepository.findBookedRoomIds(stay.checkin(), stay.checkout()));
        return packageRooms.stream()
                .filter(room -> room.getStatus() != RoomStatus.MAINTENANCE && !booked.contains(room.getId()))
                .toList();
    }

    private static List<Room> packageRooms(Tour tour) {
        return tour.getTourRooms() == null
                ? List.of()
                : tour.getTourRooms().stream().map(TourRoom::getRoom).toList();
    }

    /** The assistant names rooms by number ("101", "phòng 102"); only the package's rooms count. */
    private static List<Long> roomIdsByNumber(Tour tour, List<String> roomNumbers) {
        if (roomNumbers == null || roomNumbers.isEmpty()) {
            return List.of();
        }
        Map<String, Room> byNumber = packageRooms(tour).stream()
                .collect(Collectors.toMap(room -> room.getRoomNumber().strip().toLowerCase(), room -> room));
        List<Long> ids = new ArrayList<>();
        for (String raw : roomNumbers) {
            String number = raw.strip().toLowerCase().replaceFirst("^phòng\\s*", "");
            Room room = byNumber.get(number);
            if (room == null) {
                throw invalid("Gói tour \"" + tour.getName() + "\" không có phòng " + raw.strip() + ".");
            }
            ids.add(room.getId());
        }
        return ids;
    }

    private static List<Room> roomsOf(TourBooking booking) {
        return booking.getBooking() == null || booking.getBooking().getBookingRooms() == null
                ? List.of()
                : booking.getBooking().getBookingRooms().stream()
                        .map(BookingRoom::getRoom)
                        .sorted(ROOM_ORDER)
                        .toList();
    }

    private static Set<Long> roomIdsOf(TourBooking booking) {
        return roomsOf(booking).stream().map(Room::getId).collect(Collectors.toSet());
    }

    private static String roomList(List<Room> rooms) {
        return rooms.isEmpty()
                ? ""
                : ", phòng " + rooms.stream().map(Room::getRoomNumber).collect(Collectors.joining(", "));
    }

    private TourBookingResponse toResponse(TourBooking booking) {
        TourBookingResponse response = tourBookingMapper.toResponse(booking);
        response.setRooms(
                roomsOf(booking).stream().map(TourServiceImpl::roomResponse).toList());
        return response;
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
