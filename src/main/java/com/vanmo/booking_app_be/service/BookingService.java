package com.vanmo.booking_app_be.service;

import java.time.LocalDate;
import java.util.List;

import com.vanmo.booking_app_be.dto.request.BookingCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingUpdateRequest;
import com.vanmo.booking_app_be.dto.response.BookingResponse;
import com.vanmo.booking_app_be.dto.response.ListBookingResponse;
import com.vanmo.booking_app_be.entity.enums.PaymentStatus;

public interface BookingService {
    BookingResponse create(BookingCreateRequest req);

    BookingResponse update(Long id, BookingUpdateRequest req);

    BookingResponse getById(Long id);

    List<ListBookingResponse> getAllByHotel(Long hotelId, String bookingStatus);

    List<ListBookingResponse> getAllByCustomer(Long customerId, String bookingStatus);

    void delete(Long id);

    BookingResponse confirmBooking(Long id);

    BookingResponse cancelBooking(Long id);

    BookingResponse completeBooking(Long id);

    BookingResponse updatePaymentStatus(Long bookingId, PaymentStatus paymentStatus);

    BookingResponse changePaymentMethod(Long bookingId, String paymentMethod);

    BookingResponse markPaidByVnPay(Long bookingId);

    void markExpiredPendingPaymentsAsFailed();

    BookingResponse resetPendingPayment(Long bookingId);

    /**
     * A room booking the AI receptionist places for the guest of a conversation, after they agreed to the summary in
     * chat. There is no signed-in caller on that path (auto-reply runs on its own thread), so the conversation
     * decides the branch and the guest. Rooms are named by their number, must be free for the whole stay and belong
     * to that branch; paid at the hotel, PENDING until the branch confirms. Asking twice for the same stay returns
     * the open booking instead of a second one ({@link PlacedBooking#created()} false).
     */
    /**
     * The guest's newest room bookings at one branch, for the chat assistant's prompt. Carries no access
     * check: the only caller is the assistant, which reads the guest and the branch off the conversation
     * row it already loaded, never off anything a caller passed in.
     */
    List<BookingResponse> recentForAssistant(Long customerId, Long hotelId);

    PlacedBooking placeRoomsFromChat(
            Long conversationId, List<String> roomNumbers, LocalDate checkin, LocalDate checkout, String note);

    record PlacedBooking(BookingResponse booking, boolean created) {}
}
