package com.vanmo.booking_app_be.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vanmo.booking_app_be.dto.response.FeedbackRow;
import com.vanmo.booking_app_be.dto.response.GuestOriginDTO;
import com.vanmo.booking_app_be.dto.response.GuestOriginRow;
import com.vanmo.booking_app_be.dto.response.RatingReportResponse;

class GuestFiguresTest {

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);

    @Test
    void groupsGuestsByHometownIgnoringSpacesAndCase() {
        List<GuestOriginDTO> origins = GuestFigures.origins(
                List.of(
                        row(1, "Thái Bình", 1_000_000),
                        row(1, "Thái Bình", 500_000), // same guest again
                        row(2, "  thái  bình ", 700_000),
                        row(3, "Hà Nội", 400_000),
                        row(4, null, 300_000),
                        row(5, "   ", 100_000)),
                List.of(row(3, "Hà Nội", 900_000)));

        assertThat(origins).extracting(GuestOriginDTO::getHometown).containsExactly("Thái Bình", "Hà Nội", null);
        GuestOriginDTO thaiBinh = origins.get(0);
        assertThat(thaiBinh.getGuests()).isEqualTo(2);
        assertThat(thaiBinh.getBookings()).isEqualTo(3);
        assertThat(thaiBinh.getRevenue()).isEqualByComparingTo("2200000");
        // 2.200.000 of 3.900.000
        assertThat(thaiBinh.getShare()).isEqualTo(56.41);
        GuestOriginDTO haNoi = origins.get(1);
        assertThat(haNoi.getTours()).isEqualTo(1);
        assertThat(haNoi.getRevenue()).isEqualByComparingTo("1300000");
        // no hometown, blank or missing, is one group
        assertThat(origins.get(2).getGuests()).isEqualTo(2);
    }

    @Test
    void ratingsAverageTheStarsAndListTheLatestLowReviews() {
        List<FeedbackRow> reviews = List.of(
                review(9, 5, OCT_1.plusDays(9)),
                review(8, 2, OCT_1.plusDays(8)),
                review(7, 4, OCT_1.plusDays(7)),
                review(6, 3, OCT_1.plusDays(6)),
                review(5, 5, OCT_1.plusDays(5)));

        RatingReportResponse report =
                GuestFigures.ratings(OCT_1, OCT_1.plusDays(30), reviews, List.of(review(1, 4, OCT_1.minusDays(3))), 20);

        assertThat(report.getReviews()).isEqualTo(5);
        assertThat(report.getAverageRating()).isEqualTo(3.8);
        assertThat(report.getPreviousAverageRating()).isEqualTo(4.0);
        assertThat(report.getDistribution()).containsExactly(0L, 1L, 1L, 1L, 2L);
        assertThat(report.getReviewRate()).isEqualTo(25.0);
        assertThat(report.getLowReviews())
                .extracting(FeedbackRow::getFeedbackId)
                .containsExactly(8L, 6L);
    }

    @Test
    void noReviewsMeansNoAverage() {
        RatingReportResponse report = GuestFigures.ratings(OCT_1, OCT_1, List.of(), List.of(), 0);

        assertThat(report.getAverageRating()).isNull();
        assertThat(report.getPreviousAverageRating()).isNull();
        assertThat(report.getDistribution()).containsExactly(0L, 0L, 0L, 0L, 0L);
        assertThat(report.getReviewRate()).isZero();
    }

    private static GuestOriginRow row(long customerId, String hometown, long amount) {
        return new GuestOriginRow(customerId, hometown, BigDecimal.valueOf(amount));
    }

    private static FeedbackRow review(long id, int stars, LocalDate on) {
        return new FeedbackRow(id, id, 6L, "Hoàn Kiếm", "Khách " + id, stars, "Nhận xét " + id, on);
    }
}
