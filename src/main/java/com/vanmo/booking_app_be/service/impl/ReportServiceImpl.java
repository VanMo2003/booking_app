package com.vanmo.booking_app_be.service.impl;

import com.vanmo.booking_app_be.dto.request.ReportCreateRequest;
import com.vanmo.booking_app_be.dto.request.ReportUpdateRequest;
import com.vanmo.booking_app_be.dto.response.ReportResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.Report;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.ReportMapper;
import com.vanmo.booking_app_be.repository.ReportRepository;
import com.vanmo.booking_app_be.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ReportMapper reportMapper;

    @Override
    public ReportResponse create(ReportCreateRequest req) {
        Report entity = reportMapper.toEntity(req);
        return reportMapper.toResponse(reportRepository.save(entity));
    }

    @Override
    public ReportResponse update(Long id, ReportUpdateRequest req) {
        Report entity = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found id=" + id));
        reportMapper.updateEntity(entity, req);
        return reportMapper.toResponse(reportRepository.save(entity));
    }

    @Override
    public ReportResponse getById(Long id) {
        Report entity = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found id=" + id));
        return reportMapper.toResponse(entity);
    }

    @Override
    public PageResponse<ReportResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<ReportResponse> data = reportRepository.findAll(pageable).map(reportMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(Long id) {
        Report entity = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found id=" + id));
        reportRepository.delete(entity);
    }
}
