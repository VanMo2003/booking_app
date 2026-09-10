package com.vanmo.booking_app_be.service.impl;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final FeedbackMapper feedbackMapper;
    private final AccessGuard accessGuard;

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
        return feedbackMapper.toResponse(feedbackRepository.save(entity));
    }

    @Override
    public FeedbackResponse update(Long id, FeedbackUpdateRequest req) {
        Feedback entity = feedbackRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found id=" + id));
        feedbackMapper.updateEntity(entity, req);
        return feedbackMapper.toResponse(feedbackRepository.save(entity));
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
