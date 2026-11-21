package com.vanmo.booking_app_be.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.vanmo.booking_app_be.dto.request.FeedbackReplyRequest;
import com.vanmo.booking_app_be.dto.response.FeedbackResponse;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Booking;
import com.vanmo.booking_app_be.entity.Customer;
import com.vanmo.booking_app_be.entity.Feedback;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.enums.NotificationType;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.mapper.FeedbackMapper;
import com.vanmo.booking_app_be.repository.BookingRepository;
import com.vanmo.booking_app_be.repository.CustomerRepository;
import com.vanmo.booking_app_be.repository.FeedbackRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.NotificationService;

/**
 * The branch answering a review: who may write it, what is stamped on the row, and whether the guest hears about
 * it. The reply is public, so the guard is the interesting part.
 */
class FeedbackReplyTest {

    private final FeedbackRepository feedbackRepository = mock(FeedbackRepository.class);
    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final FeedbackMapper feedbackMapper = mock(FeedbackMapper.class);
    private final AccessGuard accessGuard = mock(AccessGuard.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    private FeedbackServiceImpl service;
    private Account manager;

    private static Hotel hotel() {
        return Hotel.builder().id(6L).name("Hoàn Kiếm Palace").build();
    }

    private static Feedback review(Customer customer) {
        return Feedback.builder()
                .id(31L)
                .rating(2)
                .comments("Phòng ồn, nhân viên nhận phòng chậm.")
                .reviewDate(LocalDate.of(2026, 11, 14))
                .booking(Booking.builder().id(88L).hotel(hotel()).build())
                .customer(customer)
                .build();
    }

    private static Customer guest(Account account) {
        return Customer.builder()
                .id(12L)
                .fullName("Trần Thu Hà")
                .account(account)
                .build();
    }

    @BeforeEach
    void setUp() {
        service = new FeedbackServiceImpl(
                feedbackRepository,
                bookingRepository,
                customerRepository,
                feedbackMapper,
                accessGuard,
                notificationService);
        manager = Account.builder().id("acc-1").username("manager1").build();
        when(accessGuard.currentAccount()).thenReturn(manager);
        when(feedbackMapper.toResponse(any()))
                .thenReturn(FeedbackResponse.builder().id(31L).build());
        when(feedbackRepository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void luu_cau_tra_loi_kem_nguoi_tra_loi_va_thoi_diem() {
        Feedback entity = review(guest(Account.builder().id("acc-9").build()));
        when(feedbackRepository.findById(31L)).thenReturn(Optional.of(entity));

        service.reply(
                31L,
                FeedbackReplyRequest.builder()
                        .reply("  Cảm ơn chị đã phản hồi, bên mình đã kê lại phòng.  ")
                        .build());

        ArgumentCaptor<Feedback> saved = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(saved.capture());
        assertThat(saved.getValue().getReply()).isEqualTo("Cảm ơn chị đã phản hồi, bên mình đã kê lại phòng.");
        assertThat(saved.getValue().getReplyAt()).isNotNull();
        assertThat(saved.getValue().getRepliedBy()).isSameAs(manager);
        // Quyền được kiểm theo cơ sở của đánh giá, không theo tham số người gọi truyền vào.
        verify(accessGuard).requireBranchManagerEditor(6L);
    }

    @Test
    void khach_co_tai_khoan_thi_nhan_thong_bao() {
        Account guestAccount = Account.builder().id("acc-9").build();
        when(feedbackRepository.findById(31L)).thenReturn(Optional.of(review(guest(guestAccount))));

        service.reply(
                31L,
                FeedbackReplyRequest.builder().reply("Cảm ơn chị đã phản hồi.").build());

        verify(notificationService)
                .notify(eq(guestAccount), eq(NotificationType.REVIEW_REPLIED), any(), any(), eq(88L));
    }

    @Test
    void khach_vang_lai_khong_co_tai_khoan_thi_khong_thong_bao_ai() {
        when(feedbackRepository.findById(31L)).thenReturn(Optional.of(review(guest(null))));

        service.reply(
                31L,
                FeedbackReplyRequest.builder().reply("Cảm ơn anh đã phản hồi.").build());

        verify(feedbackRepository).save(any());
        verify(notificationService, never()).notify(any(), any(), any(), any(), anyLong());
    }

    @Test
    void danh_gia_khong_gan_voi_co_so_nao_thi_tu_choi_truoc_khi_kiem_quyen() {
        Feedback orphan = Feedback.builder().id(31L).rating(5).build();
        when(feedbackRepository.findById(31L)).thenReturn(Optional.of(orphan));

        assertThatThrownBy(() -> service.reply(
                        31L, FeedbackReplyRequest.builder().reply("Cảm ơn anh.").build()))
                .isInstanceOf(AppException.class);

        verify(accessGuard, never()).requireBranchManagerEditor(anyLong());
        verify(feedbackRepository, never()).save(any());
    }
}
