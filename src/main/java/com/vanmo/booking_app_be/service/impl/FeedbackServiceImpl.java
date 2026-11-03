package com.vanmo.booking_app_be.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.vanmo.booking_app_be.dto.request.FeedbackCreateRequest;
import com.vanmo.booking_app_be.dto.request.FeedbackUpdateRequest;
import com.vanmo.booking_app_be.dto.response.FeedbackResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Booking;
import com.vanmo.booking_app_be.entity.Customer;
import com.vanmo.booking_app_be.entity.Feedback;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.enums.NotificationType;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.FeedbackMapper;
import com.vanmo.booking_app_be.repository.BookingRepository;
import com.vanmo.booking_app_be.repository.CustomerRepository;
import com.vanmo.booking_app_be.repository.FeedbackRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.FeedbackService;
import com.vanmo.booking_app_be.service.NotificationService;
import com.vanmo.booking_app_be.utils.NotificationMessages;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final FeedbackMapper feedbackMapper;
    private final AccessGuard accessGuard;
    private final NotificationService notificationService;

    /** A review with this many stars or fewer is sent to the chain owner and the branch manager. */
    static final int ALERT_RATING = 2;

    @Override
    public FeedbackResponse create(FeedbackCreateRequest req) {
        Booking booking = bookingRepository
                .findById(req.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found id=" + req.getBookingId()));
        Customer customer = customerRepository
                .findById(req.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found id=" + req.getCustomerId()));

        Account caller = accessGuard.currentAccount();
        if (caller.getRole() == Role.CUSTOMER
                && (customer.getAccount() == null
                        || !customer.getAccount().getId().equals(caller.getId()))) {
            throw new AppException(ErrorCode.UN_AUTHORIZED, "Customers can only leave feedback as themselves");
        }

        Feedback entity = feedbackMapper.toEntity(req);
        entity.setBooking(booking);
        entity.setCustomer(customer);
        Feedback saved = feedbackRepository.save(entity);
        alertIfLow(saved, null);
        return feedbackMapper.toResponse(saved);
    }

    @Override
    public FeedbackResponse update(Long id, FeedbackUpdateRequest req) {
        Feedback entity = feedbackRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found id=" + id));
        Integer before = entity.getRating();
        feedbackMapper.updateEntity(entity, req);
        Feedback saved = feedbackRepository.save(entity);
        alertIfLow(saved, before);
        return feedbackMapper.toResponse(saved);
    }

    /** Tells the owner and the manager about a low review - once, when it first drops to the alert level. */
    private void alertIfLow(Feedback feedback, Integer ratingBefore) {
        Integer rating = feedback.getRating();
        boolean low = rating != null && rating <= ALERT_RATING;
        boolean wasLow = ratingBefore != null && ratingBefore <= ALERT_RATING;
        if (!low
                || wasLow
                || feedback.getBooking() == null
                || feedback.getBooking().getHotel() == null) {
            return;
        }
        Hotel hotel = feedback.getBooking().getHotel();
        List<Account> recipients = new ArrayList<>();
        if (hotel.getHotelChain() != null && hotel.getHotelChain().getAccount() != null) {
            recipients.add(hotel.getHotelChain().getAccount());
        }
        if (hotel.getAccount() != null && !recipients.contains(hotel.getAccount())) {
            recipients.add(hotel.getAccount());
        }
        notificationService.notifyAccounts(
                recipients,
                NotificationType.LOW_RATING_REVIEW,
                NotificationMessages.lowRatingReview(
                        hotel.getName(),
                        feedback.getCustomer() == null
                                ? null
                                : feedback.getCustomer().getFullName(),
                        rating,
                        feedback.getComments()),
                null,
                feedback.getBooking().getId());
    }

    @Override
    public FeedbackResponse getById(Long id) {
        Feedback entity = feedbackRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found id=" + id));
        return feedbackMapper.toResponse(entity);
    }

    @Override
    public PageResponse<FeedbackResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<FeedbackResponse> data = feedbackRepository.findAll(pageable).map(feedbackMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(Long id) {
        Feedback entity = feedbackRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found id=" + id));
        feedbackRepository.delete(entity);
    }
}
