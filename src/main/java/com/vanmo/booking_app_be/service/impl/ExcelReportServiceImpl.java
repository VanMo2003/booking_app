package com.vanmo.booking_app_be.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.vanmo.booking_app_be.dto.response.*;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.service.ExcelReportService;
import com.vanmo.booking_app_be.service.ReportService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExcelReportServiceImpl implements ExcelReportService {

    /** Matches ReportServiceImpl's cap - past this the per-day sheet is skipped, not an error. */
    private static final int MAX_DAILY_RANGE = 92;

    /** Matches ReportServiceImpl's day-trend cap - past this the revenue sheet goes month by month. */
    private static final int MAX_TREND_DAYS = 400;

    /** Vietnamese names of the revenue summary's payment methods and sales channels. */
    private static final Map<String, String> SHARE_LABELS = Map.ofEntries(
            Map.entry("CASH", "Tiền mặt"),
            Map.entry("BANK_TRANSFER", "Chuyển khoản"),
            Map.entry("VN_PAY", "VNPay"),
            Map.entry("SELF_SERVICE", "Khách tự đặt trên app"),
            Map.entry("FRONT_DESK", "Đặt tại quầy"),
            Map.entry("TOUR", "Tour"),
            Map.entry("NEW", "Khách mới"),
            Map.entry("RETURNING", "Khách quay lại"),
            Map.entry("PREPAID", "Trả trước khi đến"),
            Map.entry("AT_STAY", "Trả trong thời gian ở"),
            Map.entry("AFTER_CHECKOUT", "Trả sau khi trả phòng"),
            Map.entry("UNPAID", "Chưa thu"),
            Map.entry("UNKNOWN", "Đã thu, không rõ thời điểm"));

    private static final Map<String, String> LEAD_TIME_LABELS = Map.of(
            "SAME_DAY", "Đơn huỷ trong ngày nhận phòng",
            "D1_2", "Đơn huỷ trước 1-2 ngày",
            "D3_7", "Đơn huỷ trước 3-7 ngày",
            "D8_30", "Đơn huỷ trước 8-30 ngày",
            "OVER_30", "Đơn huỷ trước hơn 30 ngày");

    private final ReportService reportService;

    @Override
    public byte[] buildHotelReport(Long hotelId, LocalDate from, LocalDate to) {
        OccupancyResponse occupancy = reportService.getOccupancy(hotelId, from, to);
        CancellationRateResponse cancellation = reportService.getCancellationRate(hotelId, from, to);

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle header = headerStyle(workbook);

            writeSummarySheet(workbook, header, "Cơ sở #" + hotelId, from, to, occupancy, cancellation);
            RevenueSummaryResponse summary = reportService.getRevenueSummary(hotelId, from, to);
            writeKpiSheet(workbook, header, summary);
            writeShares(workbook, header, summary);
            String granularity = trendGranularity(from, to);
            writeTrend(workbook, header, reportService.getRevenueTrend(hotelId, from, to, granularity), granularity);
            if (ChronoUnit.DAYS.between(from, to) <= MAX_DAILY_RANGE) {
                writeOccupancyDaily(workbook, header, reportService.getOccupancyByDate(hotelId, from, to));
            }
            writeRevenueByRoomType(workbook, header, reportService.getRevenueByRoomType(hotelId, from, to));
            writeRevenueByService(workbook, header, reportService.getRevenueByService(hotelId, from, to));
            writeRevenueByTour(workbook, header, reportService.getRevenueByTour(hotelId, from, to));
            writeTopCustomers(workbook, header, reportService.getTopCustomers(hotelId, from, to, 20));
            writeGuestOrigins(workbook, header, reportService.getGuestOrigins(hotelId, from, to));
            writeRatings(workbook, header, reportService.getRatings(hotelId, from, to), false);
            writeStaffPerformance(workbook, header, reportService.getStaffPerformance(hotelId, from, to));
            writeAiAssistant(workbook, header, reportService.getAiAssistantReport(hotelId, from, to));
            writeReceivables(workbook, header, reportService.getReceivables(hotelId), false);

            return toBytes(workbook);
        } catch (IOException e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Không tạo được file Excel: " + e.getMessage());
        }
    }

    @Override
    public byte[] buildChainReport(Long hotelChainId, LocalDate from, LocalDate to) {
        OccupancyResponse occupancy = reportService.getOccupancyForChain(hotelChainId, from, to);
        CancellationRateResponse cancellation = reportService.getCancellationRateForChain(hotelChainId, from, to);

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle header = headerStyle(workbook);

            writeSummarySheet(workbook, header, "Chuỗi #" + hotelChainId, from, to, occupancy, cancellation);
            RevenueSummaryResponse summary = reportService.getRevenueSummaryForChain(hotelChainId, from, to);
            writeKpiSheet(workbook, header, summary);
            writeBranchComparison(workbook, header, reportService.getBranchPerformanceForChain(hotelChainId, from, to));
            writeShares(workbook, header, summary);
            String granularity = trendGranularity(from, to);
            writeTrend(
                    workbook,
                    header,
                    reportService.getRevenueTrendForChain(hotelChainId, from, to, granularity),
                    granularity);
            writeRevenueByRoomType(
                    workbook, header, reportService.getRevenueByRoomTypeForChain(hotelChainId, from, to));
            writeRevenueByService(workbook, header, reportService.getRevenueByServiceForChain(hotelChainId, from, to));
            writeRevenueByTour(workbook, header, reportService.getRevenueByTourForChain(hotelChainId, from, to));
            writeTopCustomers(workbook, header, reportService.getTopCustomersForChain(hotelChainId, from, to, 20));
            writeGuestOrigins(workbook, header, reportService.getGuestOriginsForChain(hotelChainId, from, to));
            writeRatings(workbook, header, reportService.getRatingsForChain(hotelChainId, from, to), true);
            writeStaffPerformance(workbook, header, reportService.getStaffPerformanceForChain(hotelChainId, from, to));
            writeAiAssistant(workbook, header, reportService.getAiAssistantReportForChain(hotelChainId, from, to));
            writeReceivables(workbook, header, reportService.getReceivablesForChain(hotelChainId), true);

            return toBytes(workbook);
        } catch (IOException e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Không tạo được file Excel: " + e.getMessage());
        }
    }

    private void writeSummarySheet(
            Workbook workbook,
            CellStyle header,
            String scopeLabel,
            LocalDate from,
            LocalDate to,
            OccupancyResponse occupancy,
            CancellationRateResponse cancellation) {
        Sheet sheet = workbook.createSheet("Tổng quan");
        int r = 0;
        r = writeRow(sheet, r, header, "Phạm vi", scopeLabel);
        r = writeRow(sheet, r, header, "Từ ngày", from.toString());
        r = writeRow(sheet, r, header, "Đến ngày", to.toString());
        r = writeRow(sheet, r, header, "Tổng số phòng", occupancy.getTotalRooms());
        r = writeRow(sheet, r, header, "Số đêm-phòng khả dụng", occupancy.getRoomNights());
        r = writeRow(sheet, r, header, "Số đêm-phòng đã bán", occupancy.getOccupiedRoomNights());
        r = writeRow(sheet, r, header, "Tỷ lệ lấp đầy (%)", occupancy.getOccupancyRate());
        r = writeRow(sheet, r, header, "Tổng số đơn", cancellation.getTotalBooking());
        r = writeRow(sheet, r, header, "Đơn hoàn tất", cancellation.getCompletedBooking());
        r = writeRow(sheet, r, header, "Đơn bị huỷ", cancellation.getCanceledBooking());
        r = writeRow(sheet, r, header, "Tỷ lệ huỷ (%)", cancellation.getCancellationRate());
        if (cancellation.getByLeadTime() != null) {
            for (RevenueShareDTO bucket : cancellation.getByLeadTime()) {
                r = writeRow(
                        sheet,
                        r,
                        header,
                        LEAD_TIME_LABELS.getOrDefault(bucket.getKey(), bucket.getKey()),
                        bucket.getCount());
            }
            writeRow(
                    sheet, r, header, "Huỷ sát ngày, từ 2 ngày trở xuống (%)", cancellation.getLateCancellationShare());
        }
        autoSize(sheet, 2);
    }

    /** The period's KPIs next to the previous period of the same length (both ends inclusive). */
    private void writeKpiSheet(Workbook workbook, CellStyle header, RevenueSummaryResponse summary) {
        Sheet sheet = workbook.createSheet("KPI so với kỳ trước");
        RevenuePeriodDTO now = summary.getCurrent();
        RevenuePeriodDTO before = summary.getPrevious();
        writeHeader(
                sheet,
                header,
                "Chỉ số",
                "Kỳ này " + now.getFrom() + " → " + now.getTo(),
                "Kỳ trước " + before.getFrom() + " → " + before.getTo(),
                "Thay đổi");
        int r = 1;
        r = kpiRow(
                sheet,
                r,
                "Tổng doanh thu (gồm tour)",
                now.getTotalRevenue(),
                before.getTotalRevenue(),
                percent(summary.getRevenueGrowth()));
        r = kpiRow(sheet, r, "Tiền phòng", now.getRoomRevenue(), before.getRoomRevenue(), "");
        r = kpiRow(sheet, r, "Dịch vụ", now.getServiceRevenue(), before.getServiceRevenue(), "");
        r = kpiRow(sheet, r, "Tour", now.getTourRevenue(), before.getTourRevenue(), "");
        r = kpiRow(sheet, r, "Chi phí lương (phân bổ theo ngày)", now.getPayrollCost(), before.getPayrollCost(), "");
        r = kpiRow(
                sheet,
                r,
                "Lợi nhuận sau lương",
                now.getGrossProfit(),
                before.getGrossProfit(),
                percent(summary.getProfitGrowth()));
        r = kpiRow(sheet, r, "Biên lợi nhuận (%)", now.getProfitMargin(), before.getProfitMargin(), "");
        r = kpiRow(
                sheet,
                r,
                "Đơn phòng hoàn tất",
                now.getCompletedBookings(),
                before.getCompletedBookings(),
                percent(summary.getBookingGrowth()));
        r = kpiRow(sheet, r, "Đơn tour hoàn tất", now.getCompletedTours(), before.getCompletedTours(), "");
        r = kpiRow(sheet, r, "Đêm phòng đã bán", now.getRoomNightsSold(), before.getRoomNightsSold(), "");
        r = kpiRow(sheet, r, "ADR - giá TB/đêm", now.getAdr(), before.getAdr(), percent(summary.getAdrGrowth()));
        r = kpiRow(sheet, r, "RevPAR", now.getRevpar(), before.getRevpar(), percent(summary.getRevparGrowth()));
        r = kpiRow(
                sheet,
                r,
                "Lấp đầy (%)",
                now.getOccupancyRate(),
                before.getOccupancyRate(),
                points(summary.getOccupancyChange()));
        r = kpiRow(sheet, r, "Tỷ lệ huỷ (%)", now.getCancellationRate(), before.getCancellationRate(), "");
        r = kpiRow(sheet, r, "Doanh thu mất do huỷ", now.getCanceledRevenue(), before.getCanceledRevenue(), "");
        r = kpiRow(sheet, r, "Lưu trú trung bình (đêm)", now.getAverageStayNights(), before.getAverageStayNights(), "");
        r = kpiRow(sheet, r, "Đặt trước trung bình (ngày)", now.getAverageLeadDays(), before.getAverageLeadDays(), "");
        r = kpiRow(sheet, r, "Đơn có mua dịch vụ (%)", now.getServiceAttachRate(), before.getServiceAttachRate(), "");
        kpiRow(sheet, r, "Dịch vụ TB mỗi đơn", now.getServicePerBooking(), before.getServicePerBooking(), "");
        autoSize(sheet, 4);
    }

    private int kpiRow(Sheet sheet, int rowIndex, String label, Number now, Number before, String change) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(now == null ? 0d : now.doubleValue());
        row.createCell(2).setCellValue(before == null ? 0d : before.doubleValue());
        row.createCell(3).setCellValue(change);
        return rowIndex + 1;
    }

    private void writeShares(Workbook workbook, CellStyle header, RevenueSummaryResponse summary) {
        Sheet sheet = workbook.createSheet("Thanh toán & kênh bán");
        writeHeader(sheet, header, "Phương thức thanh toán", "Số đơn", "Doanh thu", "Tỷ trọng (%)");
        int r = 1;
        for (RevenueShareDTO share : summary.getByPaymentMethod()) {
            r = shareRow(sheet, r, share);
        }
        r++;
        writeHeaderAt(sheet, header, r++, "Kênh bán", "Số đơn", "Doanh thu", "Tỷ trọng (%)");
        for (RevenueShareDTO share : summary.getByChannel()) {
            r = shareRow(sheet, r, share);
        }
        if (summary.getByCustomerType() != null) {
            r++;
            writeHeaderAt(sheet, header, r++, "Loại khách", "Số đơn", "Doanh thu", "Tỷ trọng (%)");
            for (RevenueShareDTO share : summary.getByCustomerType()) {
                r = shareRow(sheet, r, share);
            }
        }
        if (summary.getByPaymentTiming() != null) {
            r++;
            writeHeaderAt(sheet, header, r++, "Thời điểm thu tiền", "Số đơn", "Doanh thu", "Tỷ trọng (%)");
            for (RevenueShareDTO share : summary.getByPaymentTiming()) {
                r = shareRow(sheet, r, share);
            }
            if (summary.getAverageDaysToCollect() != null) {
                Row line = sheet.createRow(r);
                line.createCell(0).setCellValue("Thu muộn trung bình (ngày sau trả phòng)");
                line.createCell(1).setCellValue(summary.getAverageDaysToCollect());
            }
        }
        autoSize(sheet, 4);
    }

    private int shareRow(Sheet sheet, int rowIndex, RevenueShareDTO share) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(SHARE_LABELS.getOrDefault(share.getKey(), share.getKey()));
        row.createCell(1).setCellValue(share.getCount());
        row.createCell(2).setCellValue(toDouble(share.getRevenue()));
        row.createCell(3).setCellValue(share.getShare());
        return rowIndex + 1;
    }

    private void writeBranchComparison(Workbook workbook, CellStyle header, List<BranchPerformanceDTO> rows) {
        Sheet sheet = workbook.createSheet("So sánh cơ sở");
        writeHeader(
                sheet,
                header,
                "Cơ sở",
                "Doanh thu",
                "Tỷ trọng (%)",
                "Doanh thu kỳ trước",
                "Tăng trưởng",
                "Lấp đầy (%)",
                "ADR",
                "RevPAR",
                "Tỷ lệ huỷ (%)",
                "Đơn phòng hoàn tất",
                "Đơn tour hoàn tất");
        int r = 1;
        for (BranchPerformanceDTO branch : rows) {
            RevenuePeriodDTO now = branch.getCurrent();
            Row line = sheet.createRow(r++);
            line.createCell(0).setCellValue(branch.getHotelName());
            line.createCell(1).setCellValue(toDouble(now.getTotalRevenue()));
            line.createCell(2).setCellValue(branch.getRevenueShare());
            line.createCell(3).setCellValue(toDouble(branch.getPreviousRevenue()));
            line.createCell(4).setCellValue(percent(branch.getRevenueGrowth()));
            line.createCell(5).setCellValue(now.getOccupancyRate());
            line.createCell(6).setCellValue(toDouble(now.getAdr()));
            line.createCell(7).setCellValue(toDouble(now.getRevpar()));
            line.createCell(8).setCellValue(now.getCancellationRate());
            line.createCell(9).setCellValue(now.getCompletedBookings());
            line.createCell(10).setCellValue(now.getCompletedTours());
        }
        autoSize(sheet, 11);
    }

    private void writeRevenueByTour(Workbook workbook, CellStyle header, List<RevenueByTourDTO> rows) {
        Sheet sheet = workbook.createSheet("Doanh thu theo tour");
        writeHeader(
                sheet,
                header,
                "Tour",
                "Số đơn",
                "Số khách",
                "Tiền tour",
                "Tiền phòng gói",
                "Đơn do AI lên",
                "Số chuyến",
                "Số chỗ",
                "Lấp chỗ (%)");
        int r = 1;
        for (RevenueByTourDTO row : rows) {
            Row line = sheet.createRow(r++);
            line.createCell(0).setCellValue(row.getTourName());
            line.createCell(1).setCellValue(row.getOrders());
            line.createCell(2).setCellValue(row.getGuests());
            line.createCell(3).setCellValue(toDouble(row.getTourRevenue()));
            line.createCell(4).setCellValue(toDouble(row.getPackageRoomAmount()));
            line.createCell(5).setCellValue(row.getAiOrders());
            line.createCell(6).setCellValue(row.getDepartures());
            line.createCell(7).setCellValue(row.getSeatsOffered());
            line.createCell(8).setCellValue(row.getFillRate());
        }
        autoSize(sheet, 9);
    }

    /** Today's list, whatever the export's period: completed stays whose payment was never recorded. */
    private void writeReceivables(Workbook workbook, CellStyle header, List<ReceivableDTO> rows, boolean withBranch) {
        Sheet sheet = workbook.createSheet("Tiền chưa thu");
        List<String> titles = new ArrayList<>(List.of(
                "Đơn", "Khách hàng", "Số điện thoại", "Ngày trả phòng", "Số ngày quá hạn", "Phương thức", "Số tiền"));
        if (withBranch) {
            titles.add(1, "Cơ sở");
        }
        writeHeader(sheet, header, titles.toArray(String[]::new));
        int r = 1;
        for (ReceivableDTO row : rows) {
            Row line = sheet.createRow(r++);
            int c = 0;
            line.createCell(c++).setCellValue("#" + row.getBookingId());
            if (withBranch) {
                line.createCell(c++).setCellValue(row.getHotelName());
            }
            line.createCell(c++).setCellValue(row.getCustomerName() == null ? "" : row.getCustomerName());
            line.createCell(c++).setCellValue(row.getPhoneNumber() == null ? "" : row.getPhoneNumber());
            line.createCell(c++).setCellValue(String.valueOf(row.getCheckoutDate()));
            line.createCell(c++).setCellValue(row.getDaysOverdue());
            line.createCell(c++)
                    .setCellValue(
                            row.getPaymentMethod() == null
                                    ? ""
                                    : SHARE_LABELS.getOrDefault(
                                            row.getPaymentMethod().name(),
                                            row.getPaymentMethod().name()));
            line.createCell(c).setCellValue(toDouble(row.getTotalAmount()));
        }
        autoSize(sheet, titles.size());
    }

    private void writeGuestOrigins(Workbook workbook, CellStyle header, List<GuestOriginDTO> rows) {
        Sheet sheet = workbook.createSheet("Khách theo quê quán");
        writeHeader(sheet, header, "Quê quán", "Số khách", "Đơn phòng", "Tour", "Doanh thu", "Tỷ trọng (%)");
        int r = 1;
        for (GuestOriginDTO row : rows) {
            Row line = sheet.createRow(r++);
            line.createCell(0).setCellValue(row.getHometown() == null ? "Chưa rõ" : row.getHometown());
            line.createCell(1).setCellValue(row.getGuests());
            line.createCell(2).setCellValue(row.getBookings());
            line.createCell(3).setCellValue(row.getTours());
            line.createCell(4).setCellValue(toDouble(row.getRevenue()));
            line.createCell(5).setCellValue(row.getShare());
        }
        autoSize(sheet, 6);
    }

    private void writeRatings(Workbook workbook, CellStyle header, RatingReportResponse ratings, boolean withBranch) {
        Sheet sheet = workbook.createSheet("Đánh giá");
        int r = 0;
        r = writeRow(sheet, r, header, "Số đánh giá", ratings.getReviews());
        r = writeRow(
                sheet,
                r,
                header,
                "Điểm trung bình",
                ratings.getAverageRating() == null ? "–" : ratings.getAverageRating());
        r = writeRow(
                sheet,
                r,
                header,
                "Điểm trung bình kỳ trước",
                ratings.getPreviousAverageRating() == null ? "–" : ratings.getPreviousAverageRating());
        r = writeRow(sheet, r, header, "Tỷ lệ đơn hoàn tất có đánh giá (%)", ratings.getReviewRate());
        for (int star = 5; star >= 1; star--) {
            r = writeRow(
                    sheet, r, header, star + " sao", ratings.getDistribution().get(star - 1));
        }
        r++;
        List<String> titles = new ArrayList<>(List.of("Ngày", "Khách", "Số sao", "Nhận xét", "Đơn"));
        if (withBranch) {
            titles.add(1, "Cơ sở");
        }
        writeHeaderAt(sheet, header, r++, titles.toArray(String[]::new));
        for (FeedbackRow review : ratings.getLowReviews()) {
            Row line = sheet.createRow(r++);
            int c = 0;
            line.createCell(c++).setCellValue(String.valueOf(review.getReviewDate()));
            if (withBranch) {
                line.createCell(c++).setCellValue(review.getHotelName());
            }
            line.createCell(c++).setCellValue(review.getCustomerName() == null ? "" : review.getCustomerName());
            line.createCell(c++).setCellValue(review.getRating() == null ? 0 : review.getRating());
            line.createCell(c++).setCellValue(review.getComments() == null ? "" : review.getComments());
            line.createCell(c).setCellValue("#" + review.getBookingId());
        }
        autoSize(sheet, titles.size());
    }

    private void writeAiAssistant(Workbook workbook, CellStyle header, AiAssistantReportResponse ai) {
        Sheet sheet = workbook.createSheet("Trợ lý AI");
        int r = 0;
        r = writeRow(sheet, r, header, "Hội thoại có tin nhắn trong kỳ", ai.getConversations());
        r = writeRow(sheet, r, header, "AI tự xử lý trọn", ai.getAiOnlyConversations());
        r = writeRow(sheet, r, header, "Có nhân viên tham gia", ai.getStaffConversations());
        r = writeRow(sheet, r, header, "Tin nhắn của khách", ai.getGuestMessages());
        r = writeRow(sheet, r, header, "Tin AI trả lời", ai.getAiReplies());
        r = writeRow(sheet, r, header, "Tin nhân viên trả lời", ai.getStaffReplies());
        r = writeRow(sheet, r, header, "Tỷ lệ tin do AI trả lời (%)", ai.getAiReplyShare());
        r = writeRow(sheet, r, header, "AI trả lời sau (trung vị, phút)", minutes(ai.getAiMedianReplySeconds()));
        r = writeRow(
                sheet, r, header, "Nhân viên trả lời sau (trung vị, phút)", minutes(ai.getStaffMedianReplySeconds()));
        r = writeRow(sheet, r, header, "Hội thoại đang chờ nhân viên (lúc xuất file)", ai.getWaitingForStaff());
        r = writeRow(sheet, r, header, "Đơn tour AI lên", ai.getAiTourOrders());
        r = writeRow(sheet, r, header, "  - đang mở", ai.getAiTourOrdersOpen());
        r = writeRow(sheet, r, header, "  - hoàn tất", ai.getAiTourOrdersCompleted());
        r = writeRow(sheet, r, header, "  - đã huỷ", ai.getAiTourOrdersCanceled());
        r = writeRow(sheet, r, header, "Doanh thu tour AI bán", toDouble(ai.getAiTourRevenue()));
        writeRow(sheet, r, header, "Tỷ trọng trong doanh thu tour (%)", ai.getAiTourRevenueShare());
        autoSize(sheet, 2);
    }

    /** Seconds as minutes with one decimal, or a dash when that side never replied. */
    private Object minutes(Long seconds) {
        return seconds == null ? "–" : Math.round(seconds / 6d) / 10d;
    }

    /** "+12.50%", or a dash when there was nothing to compare with. */
    private String percent(Double growth) {
        return growth == null ? "–" : String.format(Locale.ROOT, "%+.2f%%", growth);
    }

    private String points(Double change) {
        return change == null ? "–" : String.format(Locale.ROOT, "%+.2f điểm", change);
    }

    /** Day by day while the trend endpoint allows it (400 days), month by month beyond. */
    private String trendGranularity(LocalDate from, LocalDate to) {
        return ChronoUnit.DAYS.between(from, to) < MAX_TREND_DAYS ? "day" : "month";
    }

    /** Every day (or month) of the period, empty ones included: bookings, tours and their total. */
    private void writeTrend(Workbook workbook, CellStyle header, List<TrendBucketDTO> rows, String granularity) {
        boolean daily = "day".equals(granularity);
        Sheet sheet = workbook.createSheet(daily ? "Doanh thu theo ngày" : "Doanh thu theo tháng");
        writeHeader(
                sheet,
                header,
                daily ? "Ngày" : "Tháng",
                "Số đơn",
                "Tiền đơn phòng",
                "Số tour",
                "Tiền tour",
                "Tổng",
                "Chi phí lương",
                "Lợi nhuận sau lương");
        int r = 1;
        for (TrendBucketDTO row : rows) {
            Row line = sheet.createRow(r++);
            line.createCell(0)
                    .setCellValue(
                            daily
                                    ? String.valueOf(row.getStart())
                                    : String.valueOf(row.getStart()).substring(0, 7));
            line.createCell(1).setCellValue(row.getBookings());
            line.createCell(2).setCellValue(toDouble(row.getBookingRevenue()));
            line.createCell(3).setCellValue(row.getTours());
            line.createCell(4).setCellValue(toDouble(row.getTourRevenue()));
            line.createCell(5).setCellValue(toDouble(row.getTotalRevenue()));
            line.createCell(6).setCellValue(toDouble(row.getPayrollCost()));
            line.createCell(7)
                    .setCellValue(
                            toDouble(row.getGrossProfit() == null ? row.getTotalRevenue() : row.getGrossProfit()));
        }
        autoSize(sheet, 8);
    }

    private void writeOccupancyDaily(Workbook workbook, CellStyle header, List<OccupancyByDateDTO> rows) {
        Sheet sheet = workbook.createSheet("Tỷ lệ lấp đầy theo ngày");
        writeHeader(sheet, header, "Ngày", "Tổng phòng", "Phòng đã bán", "Tỷ lệ (%)");
        int r = 1;
        for (OccupancyByDateDTO row : rows) {
            Row line = sheet.createRow(r++);
            line.createCell(0).setCellValue(String.valueOf(row.getDate()));
            line.createCell(1).setCellValue(row.getTotalRooms());
            line.createCell(2).setCellValue(row.getOccupiedRooms());
            line.createCell(3).setCellValue(row.getOccupancyRate());
        }
        autoSize(sheet, 4);
    }

    private void writeRevenueByRoomType(Workbook workbook, CellStyle header, List<RevenueByRoomTypeDTO> rows) {
        Sheet sheet = workbook.createSheet("Doanh thu theo loại phòng");
        writeHeader(
                sheet,
                header,
                "Loại phòng",
                "Số phòng",
                "Lượt ở hoàn tất",
                "Số đêm-phòng",
                "Doanh thu",
                "Lấp đầy (%)",
                "ADR",
                "RevPAR",
                "Giá niêm yết TB");
        int r = 1;
        for (RevenueByRoomTypeDTO row : rows) {
            Row line = sheet.createRow(r++);
            line.createCell(0).setCellValue(row.getRoomTypeName());
            line.createCell(1).setCellValue(row.getRooms());
            line.createCell(2).setCellValue(row.getBookedRooms());
            line.createCell(3).setCellValue(row.getRoomNights());
            line.createCell(4).setCellValue(toDouble(row.getTotalRevenue()));
            line.createCell(5).setCellValue(row.getOccupancyRate());
            line.createCell(6).setCellValue(toDouble(row.getAdr()));
            line.createCell(7).setCellValue(toDouble(row.getRevpar()));
            line.createCell(8).setCellValue(toDouble(row.getListPrice()));
        }
        autoSize(sheet, 9);
    }

    private void writeRevenueByService(Workbook workbook, CellStyle header, List<RevenueByServiceDTO> rows) {
        Sheet sheet = workbook.createSheet("Doanh thu theo dịch vụ");
        writeHeader(sheet, header, "Dịch vụ", "Số lượng", "Doanh thu");
        int r = 1;
        for (RevenueByServiceDTO row : rows) {
            Row line = sheet.createRow(r++);
            line.createCell(0).setCellValue(row.getServiceName());
            line.createCell(1).setCellValue(orZero(row.getTotalQuantity()));
            line.createCell(2).setCellValue(toDouble(row.getTotalRevenue()));
        }
        autoSize(sheet, 3);
    }

    private void writeTopCustomers(Workbook workbook, CellStyle header, List<TopCustomerDTO> rows) {
        Sheet sheet = workbook.createSheet("Top khách hàng");
        writeHeader(sheet, header, "Khách hàng", "Số điện thoại", "Số đơn", "Số tour", "Tiền tour", "Tổng chi tiêu");
        int r = 1;
        for (TopCustomerDTO row : rows) {
            Row line = sheet.createRow(r++);
            line.createCell(0).setCellValue(row.getFullName());
            line.createCell(1).setCellValue(row.getPhoneNumber());
            line.createCell(2).setCellValue(orZero(row.getTotalBooking()));
            line.createCell(3).setCellValue(row.getTours());
            line.createCell(4).setCellValue(toDouble(row.getTourSpend()));
            line.createCell(5).setCellValue(toDouble(row.getTotalSpend()));
        }
        autoSize(sheet, 6);
    }

    private void writeStaffPerformance(Workbook workbook, CellStyle header, List<StaffPerformanceDTO> rows) {
        Sheet sheet = workbook.createSheet("Hiệu suất nhân viên");
        writeHeader(
                sheet,
                header,
                "Tài khoản",
                "Họ tên",
                "Chức vụ",
                "Số đơn đã nhận",
                "Giá trị đơn đã nhận",
                "Đơn hoàn tất",
                "Doanh thu hoàn tất",
                "Tour đã xử lý",
                "Doanh thu tour");
        int r = 1;
        for (StaffPerformanceDTO row : rows) {
            Row line = sheet.createRow(r++);
            line.createCell(0).setCellValue(row.getUsername() == null ? "" : row.getUsername());
            line.createCell(1).setCellValue(row.getFullName() == null ? "" : row.getFullName());
            line.createCell(2).setCellValue(row.getPositionName() == null ? "" : row.getPositionName());
            line.createCell(3).setCellValue(orZero(row.getTotalBooking()));
            line.createCell(4).setCellValue(toDouble(row.getTotalRevenue()));
            line.createCell(5).setCellValue(row.getCompletedBookings());
            line.createCell(6).setCellValue(toDouble(row.getCompletedRevenue()));
            line.createCell(7).setCellValue(row.getToursHandled());
            line.createCell(8).setCellValue(toDouble(row.getTourRevenue()));
        }
        autoSize(sheet, 9);
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font bold = workbook.createFont();
        bold.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(bold);
        return style;
    }

    private void writeHeader(Sheet sheet, CellStyle style, String... titles) {
        writeHeaderAt(sheet, style, 0, titles);
    }

    private void writeHeaderAt(Sheet sheet, CellStyle style, int rowIndex, String... titles) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < titles.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(titles[i]);
            cell.setCellStyle(style);
        }
    }

    private int writeRow(Sheet sheet, int rowIndex, CellStyle labelStyle, String label, Object value) {
        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);

        Cell valueCell = row.createCell(1);
        if (value instanceof Number number) {
            valueCell.setCellValue(number.doubleValue());
        } else {
            valueCell.setCellValue(String.valueOf(value));
        }
        return rowIndex + 1;
    }

    private void autoSize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0d : value.doubleValue();
    }

    private long orZero(Long value) {
        return value == null ? 0L : value;
    }

    private byte[] toBytes(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
