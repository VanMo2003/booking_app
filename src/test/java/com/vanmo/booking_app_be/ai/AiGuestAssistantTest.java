package com.vanmo.booking_app_be.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanmo.booking_app_be.dto.response.AiGuestAnswerResponse;
import com.vanmo.booking_app_be.entity.Booking;
import com.vanmo.booking_app_be.entity.Customer;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Tour;
import com.vanmo.booking_app_be.entity.TourBooking;
import com.vanmo.booking_app_be.entity.enums.BookingStatus;
import com.vanmo.booking_app_be.entity.enums.PaymentMethod;
import com.vanmo.booking_app_be.entity.enums.PaymentStatus;
import com.vanmo.booking_app_be.entity.enums.TourBookingStatus;
import com.vanmo.booking_app_be.entity.enums.TravelTheme;

/** What the guest's own assistant is allowed to see, and which links it is allowed to offer. */
class AiGuestAssistantTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);

    private static JsonNode input(String json) throws JsonProcessingException {
        return JSON.readTree(json);
    }

    private static Customer guest() {
        Customer customer = new Customer();
        customer.setId(3L);
        customer.setFullName("Nguyễn Văn A");
        customer.setInterests(Set.of(TravelTheme.BEACH));
        return customer;
    }

    private static Booking booking(long id) {
        return Booking.builder()
                .id(id)
                .hotel(Hotel.builder().id(6L).name("Hoàn Kiếm Palace").build())
                .checkinDate(LocalDate.of(2026, 12, 5))
                .checkoutDate(LocalDate.of(2026, 12, 7))
                .totalAmount(new BigDecimal("1800000"))
                .bookingStatus(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentMethod(PaymentMethod.CASH)
                .build();
    }

    private static TourBooking tourOrder(long id, Long tourId) {
        TourBooking order = new TourBooking();
        order.setId(id);
        order.setTourName("Food tour phố cổ");
        order.setTourDate(LocalDate.of(2026, 12, 6));
        order.setGuests(3);
        order.setChildren(1);
        order.setTotalAmount(new BigDecimal("1300000"));
        order.setStatus(TourBookingStatus.PENDING);
        order.setVoucherCode("CHAOMUNG");
        if (tourId != null) {
            Tour tour = new Tour();
            tour.setId(tourId);
            order.setTour(tour);
        }
        return order;
    }

    @Test
    void theBriefCarriesOnlyTheGuestsOwnRows() {
        String brief = brief();

        assertThat(brief).startsWith("DỮ LIỆU CỦA KHÁCH");
        assertThat(brief).contains("Khách: Nguyễn Văn A");
        assertThat(brief).contains("Sở thích du lịch: biển");
        assertThat(brief).contains("[bookings/26] #26 Hoàn Kiếm Palace: 2026-12-05 → 2026-12-07");
        assertThat(brief).contains("1.800.000 ₫, trạng thái PENDING, thanh toán UNPAID (CASH)");
        assertThat(brief).contains("[tour-bookings/9] #9 Food tour phố cổ [tours/7]");
        assertThat(brief).contains("3 khách (trong đó 1 trẻ em)");
        assertThat(brief).contains("đã dùng mã CHAOMUNG");
        assertThat(brief).contains("Mã giảm giá đang dùng được:");
    }

    @Test
    void aGuestWithNothingBookedStillGetsAReadableBrief() {
        String brief = AiGuestAssistant.brief(guest(), TODAY, List.of(), List.of(), List.of());

        assertThat(brief).contains("Đơn phòng:\n- (chưa có)");
        assertThat(brief).contains("Đơn tour:\n- (chưa có)");
    }

    @Test
    void onlyLinksToTheGuestsOwnRowsSurvive() throws JsonProcessingException {
        Set<String> allowed = AiGuestAssistant.allowedTargets(List.of(booking(26)), List.of(tourOrder(9, 7L)));
        List<String> notes = new ArrayList<>();

        AiGuestAnswerResponse answer = AiGuestAssistant.build(
                "Đơn sắp tới của tôi thế nào?",
                input(
                        """
				{
				"answer": "Bạn có đơn phòng #26 ở Hoàn Kiếm Palace, nhận phòng 05/12.",
				"suggestions": ["Xem lại đơn trong mục Đơn của tôi"],
				"links": [
					{"label": "Xem đơn phòng #26", "target": "bookingapp://bookings/26"},
					{"label": "Xem tour", "target": "tours/7"},
					{"label": "Đơn của người khác", "target": "bookings/999"},
					{"label": "Lặp lại", "target": "bookings/26"}
				]
				}
				"""),
                allowed,
                notes);

        assertThat(answer.getAnswer()).contains("#26");
        assertThat(answer.getSuggestions()).hasSize(1);
        // The bookingapp:// prefix is accepted, someone else's booking and a repeat are not.
        assertThat(answer.getLinks()).hasSize(2);
        assertThat(answer.getLinks().getFirst().getTarget()).isEqualTo("bookings/26");
        assertThat(answer.getLinks().getLast().getTarget()).isEqualTo("tours/7");
        assertThat(notes).anySatisfy(note -> assertThat(note).contains("Đã bỏ 2 liên kết"));
    }

    @Test
    void anOrderWhoseTourWasDeletedOffersNoTourLink() {
        Set<String> allowed = AiGuestAssistant.allowedTargets(List.of(), List.of(tourOrder(9, null)));

        assertThat(allowed).containsExactly("tour-bookings/9");
    }

    private static String brief() {
        return AiGuestAssistant.brief(guest(), TODAY, List.of(booking(26)), List.of(tourOrder(9, 7L)), List.of());
    }
}
