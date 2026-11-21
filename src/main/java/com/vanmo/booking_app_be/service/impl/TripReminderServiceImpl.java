package com.vanmo.booking_app_be.service.impl;

import java.time.LocalDate;
import java.util.EnumSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vanmo.booking_app_be.entity.Booking;
import com.vanmo.booking_app_be.entity.TourBooking;
import com.vanmo.booking_app_be.entity.enums.BookingStatus;
import com.vanmo.booking_app_be.entity.enums.NotificationType;
import com.vanmo.booking_app_be.entity.enums.TourBookingStatus;
import com.vanmo.booking_app_be.repository.BookingRepository;
import com.vanmo.booking_app_be.repository.TourBookingRepository;
import com.vanmo.booking_app_be.service.NotificationService;
import com.vanmo.booking_app_be.service.TripReminderService;
import com.vanmo.booking_app_be.utils.AppLinks;
import com.vanmo.booking_app_be.utils.NotificationMessages;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TripReminderServiceImpl implements TripReminderService {
    private final TourBookingRepository tourBookingRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public int sendTomorrowReminders() {
        LocalDate tomorrow = TourPricing.today().plusDays(1);
        int sent = 0;
        for (TourBooking order : tourBookingRepository.findAllByTourDateAndStatusIn(
                tomorrow, EnumSet.of(TourBookingStatus.PENDING, TourBookingStatus.CONFIRMED))) {
            if (order.getCustomer() == null || order.getCustomer().getAccount() == null) {
                continue;
            }
            notificationService.notify(
                    order.getCustomer().getAccount(),
                    NotificationType.TRIP_REMINDER,
                    NotificationMessages.tourReminder(
                            order.getTourName(),
                            order.getTourDate(),
                            order.getTour() == null ? null : order.getTour().getDeparture()),
                    AppLinks.tourBooking(order.getId()),
                    order.getId());
            sent++;
        }
        for (Booking booking : bookingRepository.findAllByCheckinDateAndBookingStatusIn(
                tomorrow, EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))) {
            // a tour's rooms are already in the tour reminder
            if (booking.getCustomer() == null
                    || booking.getCustomer().getAccount() == null
                    || tourBookingRepository
                            .findFirstByBooking_IdAndStatusIn(
                                    booking.getId(), EnumSet.of(TourBookingStatus.PENDING, TourBookingStatus.CONFIRMED))
                            .isPresent()) {
                continue;
            }
            notificationService.notify(
                    booking.getCustomer().getAccount(),
                    NotificationType.TRIP_REMINDER,
                    NotificationMessages.stayReminder(booking.getHotel().getName(), booking.getCheckinDate()),
                    AppLinks.booking(booking.getId()),
                    booking.getId());
            sent++;
        }
        return sent;
    }
}
