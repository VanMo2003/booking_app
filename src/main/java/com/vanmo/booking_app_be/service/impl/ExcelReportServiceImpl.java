package com.vanmo.booking_app_be.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

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

    private final ReportService reportService;

    @Override
    public byte[] buildHotelReport(Long hotelId, LocalDate from, LocalDate to) {
        OccupancyResponse occupancy = reportService.getOccupancy(hotelId, from, to);
        CancellationRateResponse cancellation = reportService.getCancellationRate(hotelId, from, to);

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle header = headerStyle(workbook);

            writeSummarySheet(workbook, header, "Cơ sở #" + hotelId, from, to, occupancy, cancellation);
            writeRevenueByDate(workbook, header, reportService.getRevenueByDate(hotelId, from, to));
            if (ChronoUnit.DAYS.between(from, to) <= MAX_DAILY_RANGE) {
                writeOccupancyDaily(workbook, header, reportService.getOccupancyByDate(hotelId, from, to));
            }
            writeRevenueByRoomType(workbook, header, reportService.getRevenueByRoomType(hotelId, from, to));
            writeRevenueByService(workbook, header, reportService.getRevenueByService(hotelId, from, to));
            writeTopCustomers(workbook, header, reportService.getTopCustomers(hotelId, from, to, 20));
            writeStaffPerformance(workbook, header, reportService.getStaffPerformance(hotelId, from, to));

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
            writeRevenueByDate(workbook, header, reportService.getRevenueByDateForChain(hotelChainId, from, to));
            writeRevenueByRoomType(
                    workbook, header, reportService.getRevenueByRoomTypeForChain(hotelChainId, from, to));
            writeRevenueByService(workbook, header, reportService.getRevenueByServiceForChain(hotelChainId, from, to));
            writeTopCustomers(workbook, header, reportService.getTopCustomersForChain(hotelChainId, from, to, 20));
            writeStaffPerformance(workbook, header, reportService.getStaffPerformanceForChain(hotelChainId, from, to));

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
        r = writeRow(sheet, r, header, "Đơn bị hủy", cancellation.getCanceledBooking());
        writeRow(sheet, r, header, "Tỷ lệ hủy (%)", cancellation.getCancellationRate());
        autoSize(sheet, 2);
    }

    private void writeRevenueByDate(Workbook workbook, CellStyle header, List<RevenueByDateDTO> rows) {
        Sheet sheet = workbook.createSheet("Doanh thu theo ngày");
        writeHeader(sheet, header, "Ngày", "Số đơn", "Doanh thu");
        int r = 1;
        for (RevenueByDateDTO row : rows) {
            Row line = sheet.createRow(r++);
            line.createCell(0).setCellValue(String.valueOf(row.getDate()));
            line.createCell(1).setCellValue(orZero(row.getTotalBooking()));
            line.createCell(2).setCellValue(toDouble(row.getTotalRevenue()));
        }
        autoSize(sheet, 3);
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
        writeHeader(sheet, header, "Loại phòng", "Số phòng đã bán", "Số đêm-phòng", "Doanh thu");
        int r = 1;
        for (RevenueByRoomTypeDTO row : rows) {
            Row line = sheet.createRow(r++);
            line.createCell(0).setCellValue(row.getRoomTypeName());
            line.createCell(1).setCellValue(row.getBookedRooms());
            line.createCell(2).setCellValue(row.getRoomNights());
            line.createCell(3).setCellValue(toDouble(row.getTotalRevenue()));
        }
        autoSize(sheet, 4);
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
        writeHeader(sheet, header, "Khách hàng", "Số điện thoại", "Số đơn", "Tổng chi tiêu");
        int r = 1;
        for (TopCustomerDTO row : rows) {
            Row line = sheet.createRow(r++);
            line.createCell(0).setCellValue(row.getFullName());
            line.createCell(1).setCellValue(row.getPhoneNumber());
            line.createCell(2).setCellValue(orZero(row.getTotalBooking()));
            line.createCell(3).setCellValue(toDouble(row.getTotalSpend()));
        }
        autoSize(sheet, 4);
    }

    private void writeStaffPerformance(Workbook workbook, CellStyle header, List<StaffPerformanceDTO> rows) {
        Sheet sheet = workbook.createSheet("Hiệu suất nhân viên");
        writeHeader(sheet, header, "Tài khoản", "Họ tên", "Chức vụ", "Số đơn đã nhận", "Doanh thu");
        int r = 1;
        for (StaffPerformanceDTO row : rows) {
            Row line = sheet.createRow(r++);
            line.createCell(0).setCellValue(row.getUsername());
            line.createCell(1).setCellValue(row.getFullName() == null ? "" : row.getFullName());
            line.createCell(2).setCellValue(row.getPositionName() == null ? "" : row.getPositionName());
            line.createCell(3).setCellValue(orZero(row.getTotalBooking()));
            line.createCell(4).setCellValue(toDouble(row.getTotalRevenue()));
        }
        autoSize(sheet, 5);
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font bold = workbook.createFont();
        bold.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(bold);
        return style;
    }

    private void writeHeader(Sheet sheet, CellStyle style, String... titles) {
        Row row = sheet.createRow(0);
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
