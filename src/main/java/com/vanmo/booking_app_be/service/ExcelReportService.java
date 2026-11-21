package com.vanmo.booking_app_be.service;

import java.time.LocalDate;

public interface ExcelReportService {

    /** Multi-sheet .xlsx report for one branch. Authorization is inherited from ReportService. */
    byte[] buildHotelReport(Long hotelId, LocalDate from, LocalDate to);

    /** Same report rolled up across every branch of a chain. */
    byte[] buildChainReport(Long hotelChainId, LocalDate from, LocalDate to);
}
