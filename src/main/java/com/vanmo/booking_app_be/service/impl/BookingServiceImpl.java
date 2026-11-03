package com.vanmo.booking_app_be.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.*;

import com.vanmo.booking_app_be.dto.request.BookingCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingRoomCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingServiceCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingUpdateRequest;
import com.vanmo.booking_app_be.dto.response.*;
import com.vanmo.booking_app_be.entity.*;
import com.vanmo.booking_app_be.entity.enums.BookingStatus;
import com.vanmo.booking_app_be.entity.enums.ChatSide;
import com.vanmo.booking_app_be.entity.enums.PaymentMethod;
import com.vanmo.booking_app_be.entity.enums.PaymentStatus;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.entity.enums.TourBookingStatus;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.*;
import com.vanmo.booking_app_be.repository.*;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.BookingService;

import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingRoomServiceImpl bookingRoomServiceImpl;
    private final BookingServiceServiceImpl bookingServiceServiceImpl;
    private final BookingMapper bookingMapper;
    private final HotelRepository hotelRepository;
    private final CustomerRepository customerRepository;
    private final RoomRepository roomRepository;
    private final ServiceRepository serviceRepository;
    private final RoomMapper roomMapper;
    private final ServiceMapper serviceMapper;
    private final BookingRoomMapper bookingRoomMapper;
    private final BookingServiceMapper bookingServiceMapper;
    private final BookingRoomRepository bookingRoomRepository;
    private final AccessGuard accessGuard;
    private final TourBookingRepository tourBookingRepository;

    @Override
    @Transactional
    public BookingResponse create(BookingCreateRequest req) {

        if (req.getCheckinDate().isAfter(req.getCheckoutDate())
                || req.getCheckinDate().isEqual(req.getCheckoutDate())) {
            throw new IllegalArgumentException("Checkout date must be after checkin date");
        }

        for (Long roomId : req.getRooms()) {
            boolean isBooked =
                    bookingRoomRepository.existsRoomBooked(roomId, req.getCheckinDate(), req.getCheckoutDate());

            if (isBooked) {
                throw new IllegalStateException("Room " + roomId + " is already booked in selected date range");
            }
        }

        Hotel hotel = hotelRepository.findById(req.getHotelId()).orElse(null);
        Customer customer = customerRepository.findById(req.getCustomerId()).orElse(null);

        Account caller = accessGuard.currentAccount();
        if (caller.getRole() == Role.CUSTOMER) {
            if (customer == null
                    || customer.getAccount() == null
                    || !customer.getAccount().getId().equals(caller.getId())) {
                throw new AppException(ErrorCode.UN_AUTHORIZED, "Customers can only book for themselves");
            }
        } else {
            accessGuard.requireBranchStaffOrAbove(req.getHotelId());
        }

        Booking booking = bookingMapper.toEntity(req);
        booking.setHotel(hotel);
        booking.setCustomer(customer);
        if (caller.getRole() != Role.CUSTOMER) {
            booking.setHandledBy(caller);
        }

        applyPaymentMethod(booking, booking.getPaymentMethod());

        bookingRepository.save(booking);
        BookingResponse bookingResponse = bookingMapper.toResponse(booking);
        List<BookingRoomResponse> bookingRoomResponses = new ArrayList<>();
        List<BookingServiceResponse> bookingServiceResponses = new ArrayList<>();

        for (Long room : req.getRooms()) {
            BookingRoomCreateRequest roomCreateRequest = new BookingRoomCreateRequest();
            roomCreateRequest.setBooking(booking);
            roomCreateRequest.setRoom(roomRepository.findById(room).orElse(null));
            BookingRoom bookingRoom = bookingRoomServiceImpl.createBookingRoom(roomCreateRequest);

            BookingRoomResponse bookingRoomResponse = bookingRoomMapper.toResponse(bookingRoom);
            RoomInfoResponse roomInfoResponse = roomMapper.toInfoResponse(bookingRoom.getRoom());
            bookingRoomResponse.setRoomInfo(roomInfoResponse);
            bookingRoomResponses.add(bookingRoomResponse);
        }

        for (Long service : req.getServices()) {
            com.vanmo.booking_app_be.entity.Service bookedService =
                    serviceRepository.findById(service).orElse(null);
            long quantity = 1L;

            BookingServiceCreateRequest serviceCreateRequest = new BookingServiceCreateRequest();
            serviceCreateRequest.setBooking(booking);
            serviceCreateRequest.setService(bookedService);
            serviceCreateRequest.setQuantity(quantity);
            // line total, so the revenue-by-service report has something to sum
            serviceCreateRequest.setTotal(
                    bookedService == null || bookedService.getUnitPrice() == null
                            ? BigDecimal.ZERO
                            : bookedService.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
            com.vanmo.booking_app_be.entity.BookingService bookingService =
                    bookingServiceServiceImpl.createBookingService(serviceCreateRequest);

            BookingServiceResponse bookingServiceResponse = bookingServiceMapper.toResponse(bookingService);
            ServiceInfoResponse serviceInfoResponse = serviceMapper.toInfoResponse(bookingService.getService());
            bookingServiceResponse.setServiceInfo(serviceInfoResponse);
            bookingServiceResponses.add(bookingServiceResponse);
        }

        bookingResponse.setBookingRooms(bookingRoomResponses);
        bookingResponse.setBookingServices(bookingServiceResponses);

        return bookingResponse;
    }

    @Override
    public BookingResponse update(Long id, BookingUpdateRequest req) {
        Booking entity = bookingRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found id=" + id));
        requireBookingAccess(entity);
        PaymentMethod previousMethod = entity.getPaymentMethod();
        bookingMapper.updateEntity(entity, req);
        // A new method on an unpaid booking needs its matching payment state (no VNPay timer on a cash booking).
        if (entity.getPaymentMethod() != previousMethod && entity.getPaymentStatus() != PaymentStatus.PAID) {
            applyPaymentMethod(entity, entity.getPaymentMethod());
        }
        return bookingMapper.toResponse(bookingRepository.save(entity));
    }

    @Override
    public BookingResponse getById(Long id) {
        Booking booking = bookingRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found id=" + id));
        requireBookingAccess(booking);

        return convertToBookingResponse(booking);
    }

    @Override
    public List<ListBookingResponse> getAllByHotel(Long hotelId, String bookingStatus) {
        accessGuard.requireBranchStaffOrAbove(hotelId);
        return bookingRepository.findAllByHotel_Id(hotelId).stream()
                .map(this::convertToListBookingResponse)
                .toList();
    }

    @Override
    public List<ListBookingResponse> getAllByCustomer(Long customerId, String bookingStatus) {
        Account caller = accessGuard.currentAccount();
        if (caller.getRole() == Role.CUSTOMER) {
            Customer customer = customerRepository
                    .findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found id=" + customerId));
            if (customer.getAccount() == null || !customer.getAccount().getId().equals(caller.getId())) {
                throw new AppException(ErrorCode.UN_AUTHORIZED);
            }
        }
        if (bookingStatus.isEmpty()) {
            return bookingRepository.findAllByCustomer_Id(customerId).stream()
                    .map(this::convertToListBookingResponse)
                    .toList();
        }
        return bookingRepository
                .findAllByCustomer_IdAndBookingStatus(customerId, BookingStatus.valueOf(bookingStatus))
                .stream()
                .map(this::convertToListBookingResponse)
                .toList();
    }

    ListBookingResponse convertToListBookingResponse(Booking booking) {
        ListBookingResponse listBookingResponse = bookingMapper.toListResponse(booking);
        List<BookingRoomResponse> bookingRoomResponses = booking.getBookingRooms().stream()
                .map(this::convertToBookingRoomResponse)
                .toList();

        List<BookingServiceResponse> bookingServiceResponses = booking.getBookingServices().stream()
                .map(this::convertToBookingServiceResponse)
                .toList();

        listBookingResponse.setBookingRooms(bookingRoomResponses);
        listBookingResponse.setBookingServices(bookingServiceResponses);

        return listBookingResponse;
    }

    @Override
    public void delete(Long id) {
        Booking entity = bookingRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found id=" + id));
        accessGuard.requireBranchManagerOrAbove(entity.getHotel().getId());
        bookingRepository.delete(entity);
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(Long id) {
        Booking booking =
                bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        requireBookingAccess(booking);
        if (booking.getBookingStatus().equals(BookingStatus.COMPLETED)) {
            throw new ResourceNotFoundException("Booking already completed");
        } else if (booking.getBookingStatus().equals(BookingStatus.CANCELED)) {
            throw new ResourceNotFoundException("Booking already cancelled");
        } else {
            booking.setBookingStatus(BookingStatus.CONFIRMED);
        }

        bookingRepository.saveAndFlush(booking);

        return convertToBookingResponse(booking);
    }

    @Override
    public BookingResponse cancelBooking(Long id) {
        Booking booking =
                bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        requireBookingAccess(booking);
        // Rooms sold with a tour go with the tour order: cancelling that order frees them (and tells the other side).
        tourBookingRepository
                .findFirstByBooking_IdAndStatusIn(
                        id, EnumSet.of(TourBookingStatus.PENDING, TourBookingStatus.CONFIRMED))
                .ifPresent(tour -> {
                    throw new AppException(
                            ErrorCode.TOUR_BOOKING_INVALID,
                            "Phòng này thuộc đơn tour #" + tour.getId() + ". Huỷ đơn tour để huỷ cả phòng.");
                });
        if (booking.getBookingStatus().equals(BookingStatus.COMPLETED)) {
            throw new ResourceNotFoundException("Booking already completed");
        } else {
            booking.setBookingStatus(BookingStatus.CANCELED);
            booking.setCanceledAt(new Date());
            booking.setCanceledBy(
                    accessGuard.currentAccount().getRole() == Role.CUSTOMER ? ChatSide.CUSTOMER : ChatSide.HOTEL);
        }

        bookingRepository.saveAndFlush(booking);

        return convertToBookingResponse(booking);
    }

    @Override
    public BookingResponse completeBooking(Long id) {
        Booking booking =
                bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        requireBookingAccess(booking);
        if (booking.getBookingStatus().equals(BookingStatus.CANCELED)) {
            throw new ResourceNotFoundException("Booking already cancelled");
        } else {
            booking.setBookingStatus(BookingStatus.COMPLETED);
        }

        bookingRepository.saveAndFlush(booking);

        return convertToBookingResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse updatePaymentStatus(Long bookingId, PaymentStatus paymentStatus) {
        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found id=" + bookingId));
        requireBookingAccess(booking);

        booking.setPaymentStatus(paymentStatus);

        if (paymentStatus == PaymentStatus.PAID) {
            booking.setPaidAt(java.time.LocalDateTime.now());
        }

        bookingRepository.saveAndFlush(booking);

        return convertToBookingResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse changePaymentMethod(Long bookingId, String paymentMethod) {
        PaymentMethod method = parsePaymentMethod(paymentMethod);
        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found id=" + bookingId));
        requireBookingEditor(booking);

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new AppException(ErrorCode.PAYMENT_METHOD_LOCKED, "Paid bookings keep their payment method");
        }
        if (booking.getBookingStatus() == BookingStatus.COMPLETED
                || booking.getBookingStatus() == BookingStatus.CANCELED) {
            throw new AppException(ErrorCode.PAYMENT_METHOD_LOCKED, "Closed bookings keep their payment method");
        }

        // Same method again is a no-op: re-picking VNPay must not restart a running payment window.
        if (booking.getPaymentMethod() != method) {
            applyPaymentMethod(booking, method);
            bookingRepository.saveAndFlush(booking);
        }
        return convertToBookingResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse markPaidByVnPay(Long bookingId) {
        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found id=" + bookingId));
        requireBookingAccess(booking);

        // The guest may have switched to cash after opening the VNPay page; the money still came through VNPay.
        booking.setPaymentMethod(PaymentMethod.VN_PAY);
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setPaidAt(LocalDateTime.now());

        bookingRepository.saveAndFlush(booking);
        return convertToBookingResponse(booking);
    }

    @Override
    @Transactional
    public void markExpiredPendingPaymentsAsFailed() {
        List<Booking> expiredBookings = bookingRepository.findByPaymentStatusAndPaymentExpireAtBefore(
                PaymentStatus.PENDING, java.time.LocalDateTime.now());

        for (Booking booking : expiredBookings) {
            booking.setPaymentStatus(PaymentStatus.FAILED);
        }

        bookingRepository.saveAll(expiredBookings);
    }

    @Override
    @Transactional
    public BookingResponse resetPendingPayment(Long bookingId) {
        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found id=" + bookingId));
        requireBookingAccess(booking);

        if (booking.getPaymentMethod() != PaymentMethod.VN_PAY) {
            throw new IllegalStateException("This booking does not use VN_PAY");
        }

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Booking already paid");
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELED) {
            throw new IllegalStateException("Booking is cancelled, cannot retry payment");
        }

        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Booking is completed, cannot retry payment");
        }

        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setPaymentExpireAt(java.time.LocalDateTime.now().plusMinutes(15));
        booking.setPaidAt(null);

        bookingRepository.saveAndFlush(booking);
        return convertToBookingResponse(booking);
    }

    private void requireBookingAccess(Booking booking) {
        Account caller = accessGuard.currentAccount();
        if (caller.getRole() == Role.CUSTOMER) {
            if (booking.getCustomer() == null
                    || booking.getCustomer().getAccount() == null
                    || !booking.getCustomer().getAccount().getId().equals(caller.getId())) {
                throw new AppException(ErrorCode.UN_AUTHORIZED);
            }
            return;
        }
        accessGuard.requireBranchStaffOrAbove(booking.getHotel().getId());
    }

    /** {@link #requireBookingAccess} for writes: the booking's own customer or the branch team, not the read-only admin. */
    private void requireBookingEditor(Booking booking) {
        if (accessGuard.currentAccount().getRole() == Role.CUSTOMER) {
            requireBookingAccess(booking);
            return;
        }
        accessGuard.requireBranchEditor(booking.getHotel().getId());
    }

    /**
     * The payment state that goes with a method: VNPay holds the booking 15 minutes for the online payment, cash and
     * bank transfer are settled at the hotel, so they wait unpaid with no deadline.
     */
    private static void applyPaymentMethod(Booking booking, PaymentMethod method) {
        booking.setPaymentMethod(method);
        booking.setPaidAt(null);
        if (method == PaymentMethod.VN_PAY) {
            booking.setPaymentStatus(PaymentStatus.PENDING);
            booking.setPaymentExpireAt(LocalDateTime.now().plusMinutes(15));
        } else {
            booking.setPaymentStatus(PaymentStatus.UNPAID);
            booking.setPaymentExpireAt(null);
        }
    }

    private static PaymentMethod parsePaymentMethod(String raw) {
        try {
            return PaymentMethod.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Unknown payment method: " + raw);
        }
    }

    BookingResponse convertToBookingResponse(Booking booking) {
        BookingResponse bookingResponse = bookingMapper.toResponse(booking);

        List<BookingRoomResponse> bookingRoomResponses = booking.getBookingRooms().stream()
                .map(this::convertToBookingRoomResponse)
                .toList();

        List<BookingServiceResponse> bookingServiceResponses = booking.getBookingServices().stream()
                .map(this::convertToBookingServiceResponse)
                .toList();

        bookingResponse.setBookingRooms(bookingRoomResponses);
        bookingResponse.setBookingServices(bookingServiceResponses);

        return bookingResponse;
    }

    BookingRoomResponse convertToBookingRoomResponse(BookingRoom bookingRoom) {
        BookingRoomResponse bookingRoomResponse = bookingRoomMapper.toResponse(bookingRoom);
        RoomInfoResponse roomInfoResponse = roomMapper.toInfoResponse(bookingRoom.getRoom());
        bookingRoomResponse.setRoomInfo(roomInfoResponse);

        return bookingRoomResponse;
    }

    BookingServiceResponse convertToBookingServiceResponse(
            com.vanmo.booking_app_be.entity.BookingService bookingService) {
        BookingServiceResponse bookingServiceResponse = bookingServiceMapper.toResponse(bookingService);
        ServiceInfoResponse serviceInfoResponse = serviceMapper.toInfoResponse(bookingService.getService());
        bookingServiceResponse.setServiceInfo(serviceInfoResponse);
        return bookingServiceResponse;
    }
}
