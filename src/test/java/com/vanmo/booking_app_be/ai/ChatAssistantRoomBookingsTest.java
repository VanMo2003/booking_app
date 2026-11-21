package com.vanmo.booking_app_be.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vanmo.booking_app_be.dto.response.BookingResponse;
import com.vanmo.booking_app_be.dto.response.BookingRoomResponse;
import com.vanmo.booking_app_be.dto.response.RoomInfoResponse;

/**
 * The assistant used to be told the guest's tour orders but not their room bookings, so "đơn phòng của tôi nhận
 * phòng ngày nào" had no answer in the prompt at all. These tests pin the section it now gets: the guest's own
 * rows, in the same date format as the rest of the prompt, with nothing in it the guest cannot already see in the
 * app.
 */
class ChatAssistantRoomBookingsTest {

    private static BookingResponse booking(
            Long id, String checkin, String checkout, String status, String payment, String... roomNumbers) {
        return BookingResponse.builder()
                .id(id)
                .checkinDate(checkin)
                .checkoutDate(checkout)
                .bookingStatus(status)
                .paymentStatus(payment)
                .totalAmount(new BigDecimal("1700000"))
                .bookingRooms(List.of(roomNumbers).stream()
                        .map(number -> BookingRoomResponse.builder()
                                .roomInfo(RoomInfoResponse.builder()
                                        .roomNumber(number)
                                        .build())
                                .build())
                        .toList())
                .build();
    }

    @Test
    void aGuestWithNoBookingSaysSoRatherThanLeavingTheSectionEmpty() {
        // An empty block would read as "the data is missing" and invite the model to guess.
        assertThat(ChatAssistant.roomBookingLines(List.of())).isEqualTo("- (chưa có)\n");
    }

    @Test
    void oneLinePerBookingWithTheDatesInTheSameFormatAsTheRestOfThePrompt() {
        String lines = ChatAssistant.roomBookingLines(
                List.of(booking(12L, "2026-12-20", "2026-12-22", "PENDING", "UNPAID", "101", "102")));

        assertThat(lines)
                .isEqualTo("- #12 · nhận 20/12/2026 · trả 22/12/2026 · phòng 101, 102 · 1.700.000 ₫"
                        + " · CHỜ KHÁCH SẠN XÁC NHẬN · chưa thanh toán\n");
    }

    @Test
    void everyBookingGetsItsOwnLine() {
        String lines = ChatAssistant.roomBookingLines(List.of(
                booking(12L, "2026-12-20", "2026-12-22", "CONFIRMED", "PAID", "101"),
                booking(9L, "2026-11-01", "2026-11-02", "COMPLETED", "PAID", "205")));

        assertThat(lines.lines().toList()).hasSize(2);
        assertThat(lines).contains("- #12 ").contains("- #9 ");
    }

    @Test
    void aBookingWithNoRoomRowStillReadsAsASentence() {
        BookingResponse noRooms = booking(5L, "2026-12-01", "2026-12-03", "CONFIRMED", "PAID");
        noRooms.setBookingRooms(null);

        assertThat(ChatAssistant.roomBookingLines(List.of(noRooms)))
                .isEqualTo("- #5 · nhận 01/12/2026 · trả 03/12/2026 · 1.700.000 ₫ · ĐÃ XÁC NHẬN · đã thanh toán\n");
    }

    @Test
    void aDateOrAStatusItCannotReadIsPassedThroughInsteadOfGuessed() {
        BookingResponse odd = booking(7L, "hôm nào đó", "2026-12-03", "SOMETHING_NEW", null, "301");

        String lines = ChatAssistant.roomBookingLines(List.of(odd));

        assertThat(lines).contains("nhận hôm nào đó");
        assertThat(lines).contains("SOMETHING_NEW");
        assertThat(lines).contains("chưa rõ thanh toán");
    }

    @Test
    void everyBookingStatusHasVietnameseWording() {
        assertThat(ChatAssistant.bookingStatusLabel("PAYING")).isEqualTo("CHỜ THANH TOÁN ONLINE");
        assertThat(ChatAssistant.bookingStatusLabel("PENDING")).isEqualTo("CHỜ KHÁCH SẠN XÁC NHẬN");
        assertThat(ChatAssistant.bookingStatusLabel("CONFIRMED")).isEqualTo("ĐÃ XÁC NHẬN");
        assertThat(ChatAssistant.bookingStatusLabel("COMPLETED")).isEqualTo("ĐÃ TRẢ PHÒNG");
        assertThat(ChatAssistant.bookingStatusLabel("CANCELED")).isEqualTo("ĐÃ HUỶ");
        assertThat(ChatAssistant.bookingStatusLabel(null)).isEqualTo("KHÔNG RÕ");

        assertThat(ChatAssistant.paymentLabel("PAID")).isEqualTo("đã thanh toán");
        assertThat(ChatAssistant.paymentLabel("PENDING")).isEqualTo("đang chờ thanh toán");
        assertThat(ChatAssistant.paymentLabel("UNPAID")).isEqualTo("chưa thanh toán");
        assertThat(ChatAssistant.paymentLabel("FAILED")).isEqualTo("thanh toán thất bại");
    }

    @Test
    void thePromptCarriesBothOrderSectionsAndKeepsRoomsAboveTours() {
        String prompt = ChatAssistant.prompt(
                List.of(),
                LocalDate.of(2026, 12, 19),
                ChatAssistant.Mode.SUGGEST,
                "- #3 Food tour phố cổ · 20/12/2026\n",
                "- #12 · nhận 20/12/2026 · trả 22/12/2026\n");

        assertThat(prompt).contains("Đơn phòng của khách tại cơ sở (mới nhất trước):\n- #12 ");
        assertThat(prompt).contains("Đơn tour của khách tại cơ sở (mới nhất trước):\n- #3 ");
        assertThat(prompt.indexOf("Đơn phòng của khách")).isLessThan(prompt.indexOf("Đơn tour của khách"));
    }
}
