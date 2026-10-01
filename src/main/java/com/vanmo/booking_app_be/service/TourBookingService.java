package com.vanmo.booking_app_be.service;

import java.time.LocalDate;
import java.util.List;

import com.vanmo.booking_app_be.dto.request.TourBookingCreateRequest;
import com.vanmo.booking_app_be.dto.response.TourBookingResponse;
import com.vanmo.booking_app_be.dto.response.TourRoomAvailabilityResponse;
import com.vanmo.booking_app_be.entity.enums.TourBookingStatus;

public interface TourBookingService {
    /** A guest orders a tour for themselves from the app. */
    TourBookingResponse create(TourBookingCreateRequest req);

    /**
     * The AI receptionist orders a tour for the guest of a conversation, once the guest agreed in chat. No caller
     * check: the assistant runs outside any request. Asking again for the same tour, date and head count returns the
     * open order instead of a second one ({@link Placed#created()} false).
     */
    Placed placeFromChat(
            Long conversationId, Long tourId, LocalDate tourDate, int guests, List<String> roomNumbers, String note);

    /** The stay that goes with the tour from this date, and which of its rooms are free for it. */
    TourRoomAvailabilityResponse availableRooms(Long tourId, LocalDate tourDate);

    List<TourBookingResponse> getMine();

    List<TourBookingResponse> getByHotel(Long hotelId, TourBookingStatus status);

    TourBookingResponse getById(Long id);

    TourBookingResponse confirm(Long id);

    TourBookingResponse complete(Long id);

    /** The guest cancels their own order, or the branch team cancels it with a reason the guest is told. */
    TourBookingResponse cancel(Long id, String reason);

    /** The guest's latest orders at the branch, newest first, so the assistant knows what was already booked. */
    List<TourBookingResponse> recentForAssistant(Long customerId, Long hotelId);

    record Placed(TourBookingResponse booking, boolean created) {}
}
