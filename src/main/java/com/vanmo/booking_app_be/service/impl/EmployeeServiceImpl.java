package com.vanmo.booking_app_be.service.impl;

import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.vanmo.booking_app_be.dto.request.AccountCreateRequest;
import com.vanmo.booking_app_be.dto.request.EmployeeCreateRequest;
import com.vanmo.booking_app_be.dto.request.EmployeeUpdateRequest;
import com.vanmo.booking_app_be.dto.response.EmployeeResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Employee;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Position;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.EmployeeMapper;
import com.vanmo.booking_app_be.repository.AccountRepository;
import com.vanmo.booking_app_be.repository.EmployeeRepository;
import com.vanmo.booking_app_be.repository.HotelRepository;
import com.vanmo.booking_app_be.repository.PositionRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.AccountService;
import com.vanmo.booking_app_be.service.EmployeeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final AccountRepository accountRepository;
    private final HotelRepository hotelRepository;
    private final PositionRepository positionRepository;
    private final AccountService accountService;
    private final AccountServiceImpl accountServiceImpl;
    private final AccessGuard accessGuard;

    @Transactional
    @Override
    public EmployeeResponse create(EmployeeCreateRequest req) {
        // Creates a STAFF login account, which admins may not do.
        Hotel hotel = accessGuard.requireBranchManagerEditor(req.getHotelId());

        AccountCreateRequest accountCreateRequest = AccountCreateRequest.builder()
                .username(req.getUsername())
                .password(req.getPassword())
                .role(Role.STAFF.name())
                .build();

        Account account = accountServiceImpl.createAccount(accountCreateRequest);

        Position position = positionRepository
                .findById(req.getPositionId())
                .orElseThrow(() -> new ResourceNotFoundException("Position not found id"));

        Employee entity = employeeMapper.toEntity(req);
        entity.setAccount(account);
        entity.setHotel(hotel);
        entity.setPosition(position);

        employeeRepository.save(entity);
        return employeeMapper.toResponse(entity);
    }

    @Override
    public EmployeeResponse update(Long id, EmployeeUpdateRequest req) {
        Employee entity = employeeRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found id=" + id));
        accessGuard.requireBranchManagerOrAbove(entity.getHotel().getId());
        employeeMapper.updateEntity(entity, req);
        return employeeMapper.toResponse(employeeRepository.save(entity));
    }

    @Override
    public EmployeeResponse getById(Long id) {
        Employee entity = employeeRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found id=" + id));
        accessGuard.requireBranchStaffOrAbove(entity.getHotel().getId());
        return employeeMapper.toResponse(entity);
    }

    @Override
    public List<EmployeeResponse> getAllByHotelId(Long hotelId) {
        accessGuard.requireBranchStaffOrAbove(hotelId);
        List<EmployeeResponse> data = employeeRepository.findAllByHotelId(hotelId).stream()
                .map(employee -> {
                    EmployeeResponse employeeResponse = employeeMapper.toResponse(employee);
                    employeeResponse.setPositionName(employee.getPosition().getName());

                    return employeeResponse;
                })
                .toList();
        return data;
    }

    @Override
    public PageResponse<EmployeeResponse> getAll(int page, int size) {
        accessGuard.requireAdmin();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<EmployeeResponse> data = employeeRepository.findAll(pageable).map(employeeMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(Long id) {
        Employee entity = employeeRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found id=" + id));
        accessGuard.requireBranchManagerOrAbove(entity.getHotel().getId());
        employeeRepository.delete(entity);
    }
}
