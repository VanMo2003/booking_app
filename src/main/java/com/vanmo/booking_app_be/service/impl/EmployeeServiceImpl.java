package com.vanmo.booking_app_be.service.impl;

import com.vanmo.booking_app_be.dto.request.EmployeeCreateRequest;
import com.vanmo.booking_app_be.dto.request.EmployeeUpdateRequest;
import com.vanmo.booking_app_be.dto.response.EmployeeResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Employee;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Position;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.EmployeeMapper;
import com.vanmo.booking_app_be.repository.AccountRepository;
import com.vanmo.booking_app_be.repository.EmployeeRepository;
import com.vanmo.booking_app_be.repository.HotelRepository;
import com.vanmo.booking_app_be.repository.PositionRepository;
import com.vanmo.booking_app_be.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final AccountRepository accountRepository;
    private final HotelRepository hotelRepository;
    private final PositionRepository positionRepository;
    @Override
    public EmployeeResponse create(EmployeeCreateRequest req) {
        Account account = accountRepository.findById(req.getAccountId()).orElseThrow(() -> new ResourceNotFoundException("Account not found id"));
        Hotel hotel = hotelRepository.findById(req.getHotelId()).orElseThrow(() -> new ResourceNotFoundException("Hotel not found id"));
        Position position = positionRepository.findById(req.getPositionId()).orElseThrow(() -> new ResourceNotFoundException("Position not found id"));

        Employee entity = employeeMapper.toEntity(req);
        entity.setAccount(account);
        entity.setHotel(hotel);
        entity.setPosition(position);

        employeeRepository.save(entity);
        return employeeMapper.toResponse(entity);
    }

    @Override
    public EmployeeResponse update(Long id, EmployeeUpdateRequest req) {
        Employee entity = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found id=" + id));
        employeeMapper.updateEntity(entity, req);
        return employeeMapper.toResponse(employeeRepository.save(entity));
    }

    @Override
    public EmployeeResponse getById(Long id) {
        Employee entity = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found id=" + id));
        return employeeMapper.toResponse(entity);
    }

    @Override
    public List<EmployeeResponse> getAllByHotelId(Long hotelId) {
        List<EmployeeResponse> data = employeeRepository.findAllByHotelId(hotelId).stream().map(employee -> {
            EmployeeResponse employeeResponse = employeeMapper.toResponse(employee);
            employeeResponse.setPositionName(employee.getPosition().getName());

            return employeeResponse;
        }).toList();
        return data;
    }

    @Override
    public PageResponse<EmployeeResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<EmployeeResponse> data = employeeRepository.findAll(pageable).map(employeeMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(Long id) {
        Employee entity = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found id=" + id));
        employeeRepository.delete(entity);
    }
}
