package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.ReportCreateRequest;
import com.vanmo.booking_app_be.dto.request.ReportUpdateRequest;
import com.vanmo.booking_app_be.dto.response.ReportResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

public interface ReportService {
    ReportResponse create(ReportCreateRequest req);
    ReportResponse update(Long id, ReportUpdateRequest req);
    ReportResponse getById(Long id);
    PageResponse<ReportResponse> getAll(int page, int size);
    void delete(Long id);
}
