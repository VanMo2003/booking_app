package com.vanmo.booking_app_be.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vanmo.booking_app_be.dto.response.BranchPerformanceDTO;
import com.vanmo.booking_app_be.dto.response.CancellationRateResponse;
import com.vanmo.booking_app_be.dto.response.OccupancyResponse;
import com.vanmo.booking_app_be.dto.response.RevenueByTourDTO;
import com.vanmo.booking_app_be.dto.response.RevenuePeriodDTO;
import com.vanmo.booking_app_be.dto.response.RevenueShareDTO;
import com.vanmo.booking_app_be.dto.response.RevenueSummaryResponse;
import com.vanmo.booking_app_be.service.ReportService;

class ExcelReportServiceImplTest {

    private static final LocalDate FROM = LocalDate.of(2026, 10, 1);
    private static final LocalDate TO = LocalDate.of(2026, 10, 20);

    private final ReportService reports = mock(ReportService.class);
    private final ExcelReportServiceImpl excel = new ExcelReportServiceImpl(reports);

    private static RevenuePeriodDTO period(LocalDate from, LocalDate to, long total) {
        return RevenuePeriodDTO.builder()
                .from(from)
                .to(to)
                .totalRevenue(BigDecimal.valueOf(total))
                .roomRevenue(BigDecimal.valueOf(total))
                .serviceRevenue(BigDecimal.ZERO)
                .tourRevenue(BigDecimal.ZERO)
                .adr(BigDecimal.valueOf(600_000))
                .revpar(BigDecimal.valueOf(3_000))
                .averagePerBooking(BigDecimal.ZERO)
                .occupancyRate(12.5)
                .build();
    }

    private static RevenueSummaryResponse summary() {
        return RevenueSummaryResponse.builder()
                .current(period(FROM, TO, 3_000_000))
                .previous(period(FROM.minusDays(20), FROM.minusDays(1), 2_000_000))
                .revenueGrowth(50d)
                .occupancyChange(2.5)
                .byPaymentMethod(new ArrayList<>(List.of(
                        new RevenueShareDTO("CASH", 1, BigDecimal.valueOf(1_000_000), 33.33),
                        new RevenueShareDTO("VN_PAY", 1, BigDecimal.valueOf(2_000_000), 66.67))))
                .byChannel(new ArrayList<>(
                        List.of(new RevenueShareDTO("FRONT_DESK", 2, BigDecimal.valueOf(3_000_000), 100d))))
                .build();
    }

    @BeforeEach
    void stubTotals() {
        OccupancyResponse occupancy =
                OccupancyResponse.builder().from(FROM).to(TO).totalRooms(10).build();
        CancellationRateResponse cancellation =
                CancellationRateResponse.builder().from(FROM).to(TO).build();
        when(reports.getOccupancy(anyLong(), any(), any())).thenReturn(occupancy);
        when(reports.getOccupancyForChain(anyLong(), any(), any())).thenReturn(occupancy);
        when(reports.getCancellationRate(anyLong(), any(), any())).thenReturn(cancellation);
        when(reports.getCancellationRateForChain(anyLong(), any(), any())).thenReturn(cancellation);
        when(reports.getRevenueSummary(anyLong(), any(), any())).thenReturn(summary());
        when(reports.getRevenueSummaryForChain(anyLong(), any(), any())).thenReturn(summary());
        when(reports.getRevenueByTour(anyLong(), any(), any()))
                .thenReturn(List.of(new RevenueByTourDTO(
                        7L, "Hạ Long", 1, 2, BigDecimal.valueOf(900_000), BigDecimal.valueOf(550_000), 1)));
    }

    private static Workbook read(byte[] bytes) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    @Test
    void branchWorkbookHasKpisVersusThePreviousPeriodSharesAndTours() throws IOException {
        try (Workbook book = read(excel.buildHotelReport(6L, FROM, TO))) {
            Sheet kpi = book.getSheet("KPI so với kỳ trước");
            assertThat(kpi).isNotNull();
            assertThat(kpi.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Tổng doanh thu (gồm tour)");
            assertThat(kpi.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(3_000_000d);
            assertThat(kpi.getRow(1).getCell(2).getNumericCellValue()).isEqualTo(2_000_000d);
            assertThat(kpi.getRow(1).getCell(3).getStringCellValue()).isEqualTo("+50.00%");
            // no previous ADR to compare with
            assertThat(kpi.getRow(6).getCell(0).getStringCellValue()).isEqualTo("Lợi nhuận sau lương");
            assertThat(kpi.getRow(11).getCell(0).getStringCellValue()).isEqualTo("ADR - giá TB/đêm");
            assertThat(kpi.getRow(11).getCell(3).getStringCellValue()).isEqualTo("–");
            assertThat(kpi.getRow(13).getCell(3).getStringCellValue()).isEqualTo("+2.50 điểm");

            // one spelling of "huỷ" across sheets, like the app
            Sheet overview = book.getSheet("Tổng quan");
            assertThat(overview.getRow(10).getCell(0).getStringCellValue()).isEqualTo("Tỷ lệ huỷ (%)");

            Sheet shares = book.getSheet("Thanh toán & kênh bán");
            assertThat(shares.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Tiền mặt");
            assertThat(shares.getRow(4).getCell(0).getStringCellValue()).isEqualTo("Kênh bán");
            assertThat(shares.getRow(5).getCell(0).getStringCellValue()).isEqualTo("Đặt tại quầy");

            Sheet tours = book.getSheet("Doanh thu theo tour");
            assertThat(tours.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Hạ Long");
            assertThat(tours.getRow(1).getCell(3).getNumericCellValue()).isEqualTo(900_000d);
            assertThat(book.getSheet("So sánh cơ sở")).isNull();
        }
    }

    @Test
    void chainWorkbookAlsoComparesTheBranches() throws IOException {
        when(reports.getBranchPerformanceForChain(anyLong(), any(), any()))
                .thenReturn(List.of(BranchPerformanceDTO.builder()
                        .hotelId(6L)
                        .hotelName("Hoàn Kiếm")
                        .current(period(FROM, TO, 3_000_000))
                        .previousRevenue(BigDecimal.ZERO)
                        .revenueShare(100d)
                        .build()));

        try (Workbook book = read(excel.buildChainReport(10L, FROM, TO))) {
            Sheet branches = book.getSheet("So sánh cơ sở");
            assertThat(branches.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Hoàn Kiếm");
            assertThat(branches.getRow(1).getCell(2).getNumericCellValue()).isEqualTo(100d);
            assertThat(branches.getRow(1).getCell(4).getStringCellValue()).isEqualTo("–");
            assertThat(book.getSheetIndex("So sánh cơ sở")).isEqualTo(book.getSheetIndex("KPI so với kỳ trước") + 1);
        }
    }
}
