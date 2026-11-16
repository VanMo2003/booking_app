package com.vanmo.booking_app_be.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanmo.booking_app_be.dto.response.BranchPerformanceDTO;
import com.vanmo.booking_app_be.dto.response.RevenueByTourDTO;
import com.vanmo.booking_app_be.dto.response.RevenueOutlookResponse;
import com.vanmo.booking_app_be.dto.response.RevenuePeriodDTO;
import com.vanmo.booking_app_be.dto.response.RevenueShareDTO;
import com.vanmo.booking_app_be.dto.response.RevenueSummaryResponse;

/** The period the question is about, and the brief the model is allowed to answer from. */
class AiReportAnalystTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);

    private static JsonNode input(String json) throws JsonProcessingException {
        return JSON.readTree(json);
    }

    private static RevenuePeriodDTO period(LocalDate from, LocalDate to, String revenue) {
        RevenuePeriodDTO period = new RevenuePeriodDTO();
        period.setFrom(from);
        period.setTo(to);
        period.setTotalRevenue(new BigDecimal(revenue));
        period.setRoomRevenue(new BigDecimal(revenue));
        period.setOccupancyRate(42.5);
        period.setAdr(new BigDecimal("850000"));
        period.setCompletedBookings(12);
        return period;
    }

    @Test
    void noPeriodInTheQuestionMeansThisMonthToDate() throws JsonProcessingException {
        List<String> notes = new ArrayList<>();

        AiReportAnalyst.Period period = AiReportAnalyst.period(input("{}"), TODAY, notes);

        assertThat(period.from()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(period.to()).isEqualTo(TODAY);
        assertThat(notes).isEmpty();
    }

    @Test
    void readsAPeriodAndKeepsItInsideWhatHappened() throws JsonProcessingException {
        List<String> notes = new ArrayList<>();

        AiReportAnalyst.Period period = AiReportAnalyst.period(
                input("""
				{"from": "2026-10-01", "to": "2026-12-31", "topic": "doanh thu"}
				"""), TODAY, notes);

        assertThat(period.from()).isEqualTo(LocalDate.of(2026, 10, 1));
        // The rest of December has not happened yet.
        assertThat(period.to()).isEqualTo(TODAY);
        assertThat(period.topic()).isEqualTo("doanh thu");
    }

    @Test
    void aPeriodStillAheadFallsBackToThisMonth() throws JsonProcessingException {
        List<String> notes = new ArrayList<>();

        AiReportAnalyst.Period period =
                AiReportAnalyst.period(input("""
				{"from": "2027-01-01", "to": "2027-01-31"}
				"""), TODAY, notes);

        assertThat(period.from()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(period.to()).isEqualTo(TODAY);
        assertThat(notes).anySatisfy(note -> assertThat(note).contains("tương lai"));
    }

    @Test
    void swappedDatesAndTooLongAPeriodAreFixed() throws JsonProcessingException {
        List<String> notes = new ArrayList<>();

        AiReportAnalyst.Period swapped =
                AiReportAnalyst.period(input("""
				{"from": "2026-11-10", "to": "2026-11-01"}
				"""), TODAY, notes);
        assertThat(swapped.from()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(swapped.to()).isEqualTo(LocalDate.of(2026, 11, 10));

        AiReportAnalyst.Period long_ =
                AiReportAnalyst.period(input("""
				{"from": "2020-01-01", "to": "2026-11-16"}
				"""), TODAY, notes);
        assertThat(long_.from()).isEqualTo(TODAY.minusDays(AiReportAnalyst.MAX_DAYS));
        assertThat(notes).anySatisfy(note -> assertThat(note).contains("quá dài"));
    }

    @Test
    void theBriefCarriesTheFiguresAndNothingElse() {
        RevenueSummaryResponse summary = new RevenueSummaryResponse();
        summary.setCurrent(period(LocalDate.of(2026, 11, 1), TODAY, "120000000"));
        summary.setPrevious(period(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 16), "100000000"));
        summary.setRevenueGrowth(20.0);
        // A growth the reports could not work out stays null and must be said in words, not as 0%.
        summary.setAdrGrowth(null);
        RevenueShareDTO channel = new RevenueShareDTO();
        channel.setKey("SELF_SERVICE");
        channel.setRevenue(new BigDecimal("72000000"));
        channel.setShare(60);
        summary.setByChannel(List.of(channel));

        RevenueByTourDTO tour = new RevenueByTourDTO();
        tour.setTourName("Du thuyền vịnh Hạ Long");
        tour.setTourRevenue(new BigDecimal("18000000"));
        tour.setOrders(9);
        tour.setGuests(21);
        tour.setAiOrders(3);

        BranchPerformanceDTO branch = new BranchPerformanceDTO();
        branch.setHotelName("Hạ Long Bay Hotel");
        branch.setCurrent(period(LocalDate.of(2026, 11, 1), TODAY, "70000000"));
        branch.setRevenueShare(58.3);
        branch.setRevenueGrowth(12.0);
        branch.setReviews(4);
        branch.setAverageRating(4.5);

        RevenueOutlookResponse outlook = new RevenueOutlookResponse();
        outlook.setTotalExpected(new BigDecimal("30000000"));
        outlook.setBookings(7);
        outlook.setTours(2);
        outlook.setOccupancyOnTheBooks(31.0);

        String brief = AiReportAnalyst.brief(
                true,
                new AiReportAnalyst.Period(LocalDate.of(2026, 11, 1), TODAY, "doanh thu"),
                summary,
                List.of(tour),
                List.of(branch),
                outlook);

        assertThat(brief).startsWith("SỐ LIỆU");
        assertThat(brief).contains("Phạm vi: toàn chuỗi");
        assertThat(brief).contains("120.000.000 ₫");
        assertThat(brief).contains("Kỳ trước (để so sánh): 2026-10-01");
        assertThat(brief).contains("- Doanh thu: 20,0%");
        assertThat(brief).contains("ADR: chưa so sánh được");
        assertThat(brief).contains("Theo kênh bán: SELF_SERVICE 72.000.000 ₫ (60,0%)");
        assertThat(brief).contains("Du thuyền vịnh Hạ Long: 18.000.000 ₫, 9 đơn, 21 khách, 3 đơn do trợ lý AI");
        assertThat(brief).contains("Hạ Long Bay Hotel: 70.000.000 ₫ (58,3% doanh thu chuỗi)");
        assertThat(brief).contains("4 đánh giá trung bình 4,5 sao");
        assertThat(brief).contains("Đã đặt trước cho 14 ngày tới: 30.000.000 ₫");
    }

    @Test
    void aMissingFigureReadsAsZeroNotAnError() {
        assertThat(AiReportAnalyst.money(null)).isEqualTo("0 ₫");
        assertThat(AiReportAnalyst.money(new BigDecimal("1250000"))).isEqualTo("1.250.000 ₫");
        assertThat(AiReportAnalyst.number(42.5)).isEqualTo("42,5");
    }
}
