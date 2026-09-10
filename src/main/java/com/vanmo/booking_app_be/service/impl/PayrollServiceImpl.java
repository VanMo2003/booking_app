package com.vanmo.booking_app_be.service.impl;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.vanmo.booking_app_be.dto.request.PayrollCreateRequest;
import com.vanmo.booking_app_be.dto.request.PayrollUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.PayrollResponse;
import com.vanmo.booking_app_be.entity.Employee;
import com.vanmo.booking_app_be.entity.Payroll;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.PayrollMapper;
import com.vanmo.booking_app_be.repository.EmployeeRepository;
import com.vanmo.booking_app_be.repository.PayrollRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.PayrollService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;
    private final PayrollMapper payrollMapper;
    private final EmployeeRepository employeeRepository;
    private final AccessGuard accessGuard;

    @Override
    public PayrollResponse create(PayrollCreateRequest req) {
        Employee employee = employeeRepository
                .findById(req.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found id=" + req.getEmployeeId()));
        accessGuard.requireBranchManagerOrAbove(employee.getHotel().getId());

        Payroll entity = payrollMapper.toEntity(req);
        entity.setEmployee(employee);
        return payrollMapper.toResponse(payrollRepository.save(entity));
    }

    @Override
    public PayrollResponse update(Long id, PayrollUpdateRequest req) {
        Payroll entity = payrollRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found id=" + id));
        accessGuard.requireBranchManagerOrAbove(entity.getEmployee().getHotel().getId());
        payrollMapper.updateEntity(entity, req);
        return payrollMapper.toResponse(payrollRepository.save(entity));
    }

    @Override
    public PayrollResponse getById(Long id) {
        Payroll entity = payrollRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found id=" + id));
        accessGuard.requireBranchManagerOrAbove(entity.getEmployee().getHotel().getId());
        return payrollMapper.toResponse(entity);
    }

    @Override
    public PageResponse<PayrollResponse> getAll(int page, int size) {
        accessGuard.requireAdmin();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<PayrollResponse> data = payrollRepository.findAll(pageable).map(payrollMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(Long id) {
        Payroll entity = payrollRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found id=" + id));
        accessGuard.requireBranchManagerOrAbove(entity.getEmployee().getHotel().getId());
        payrollRepository.delete(entity);
    }
}
