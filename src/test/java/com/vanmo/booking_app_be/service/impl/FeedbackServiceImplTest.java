package com.vanmo.booking_app_be.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.vanmo.booking_app_be.dto.request.FeedbackCreateRequest;
import com.vanmo.booking_app_be.dto.request.FeedbackUpdateRequest;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Booking;
import com.vanmo.booking_app_be.entity.Customer;
import com.vanmo.booking_app_be.entity.Feedback;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.HotelChain;
import com.vanmo.booking_app_be.entity.enums.NotificationType;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.mapper.FeedbackMapper;
import com.vanmo.booking_app_be.repository.BookingRepository;
import com.vanmo.booking_app_be.repository.CustomerRepository;
import com.vanmo.booking_app_be.repository.FeedbackRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.NotificationService;
import com.vanmo.booking_app_be.utils.NotificationMessages;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeedbackServiceImplTest {

    @Mock
    FeedbackRepository feedbackRepository;

    @Mock
    BookingRepository bookingRepository;

    @Mock
    CustomerRepository customerRepository;

    @Mock
    FeedbackMapper feedbackMapper;

    @Mock
    AccessGuard accessGuard;

    @Mock
    NotificationService notificationService;

    @InjectMocks
    FeedbackServiceImpl service;

    private final Account owner =
            Account.builder().id("owner").role(Role.HOTEL_OWNER).build();
    private final Account manager =
            Account.builder().id("manager").role(Role.HOTEL_MANAGER).build();
    private final Account guest =
            Account.builder().id("guest").role(Role.CUSTOMER).build();
    private Booking booking;
    private Customer customer;

    @BeforeEach
    void setUp() {
        Hotel hotel = Hotel.builder()
                .id(6L)
                .name("Hoàn Kiếm Palace")
                .account(manager)
                .hotelChain(HotelChain.builder().id(10L).account(owner).build())
                .build();
        booking = Booking.builder().id(21L).hotel(hotel).build();
        customer =
                Customer.builder().id(13L).fullName("Nguyễn An").account(guest).build();
        when(bookingRepository.findById(21L)).thenReturn(Optional.of(booking));
        when(customerRepository.findById(13L)).thenReturn(Optional.of(customer));
        when(accessGuard.currentAccount()).thenReturn(guest);
        when(feedbackRepository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void aOneStarReviewIsSentToTheOwnerAndTheManager() {
        when(feedbackMapper.toEntity(any()))
                .thenReturn(Feedback.builder().rating(1).comments("Phòng bẩn").build());

        service.create(request());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Account>> to = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<NotificationMessages.Message> message =
                ArgumentCaptor.forClass(NotificationMessages.Message.class);
        verify(notificationService)
                .notifyAccounts(
                        to.capture(), eq(NotificationType.LOW_RATING_REVIEW), message.capture(), any(), eq(21L));
        assertThat(to.getValue()).containsExactly(owner, manager);
        assertThat(message.getValue().title()).isEqualTo("Đánh giá 1 sao · Hoàn Kiếm Palace");
        assertThat(message.getValue().body()).contains("Nguyễn An đánh giá 1/5").contains("Phòng bẩn");
    }

    @Test
    void aGoodReviewSendsNothing() {
        when(feedbackMapper.toEntity(any()))
                .thenReturn(Feedback.builder().rating(4).build());

        service.create(request());

        verify(notificationService, never()).notifyAccounts(anyList(), any(), any(), any(), any());
    }

    @Test
    void loweringAReviewToTwoStarsAlertsOnceButEditingALowOneDoesNot() {
        Feedback review = Feedback.builder()
                .id(3L)
                .rating(4)
                .booking(booking)
                .customer(customer)
                .build();
        when(feedbackRepository.findById(3L)).thenReturn(Optional.of(review));
        org.mockito.Mockito.doAnswer(call -> {
                    review.setRating(2);
                    return null;
                })
                .when(feedbackMapper)
                .updateEntity(eq(review), any());

        service.update(3L, new FeedbackUpdateRequest());
        service.update(3L, new FeedbackUpdateRequest());

        verify(notificationService, org.mockito.Mockito.times(1))
                .notifyAccounts(anyList(), eq(NotificationType.LOW_RATING_REVIEW), any(), any(), eq(21L));
    }

    private static FeedbackCreateRequest request() {
        FeedbackCreateRequest req = new FeedbackCreateRequest();
        req.setBookingId(21L);
        req.setCustomerId(13L);
        return req;
    }
}
