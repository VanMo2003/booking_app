package com.vanmo.booking_app_be.service.impl;

import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.vanmo.booking_app_be.dto.request.HotelChainCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelChainUpdateRequest;
import com.vanmo.booking_app_be.dto.response.HotelChainDetailResponse;
import com.vanmo.booking_app_be.dto.response.ListHotelChainResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.HotelChain;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.HotelChainMapper;
import com.vanmo.booking_app_be.mapper.HotelMapper;
import com.vanmo.booking_app_be.repository.AccountRepository;
import com.vanmo.booking_app_be.repository.HotelChainRepository;
import com.vanmo.booking_app_be.repository.HotelRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.HotelChainService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HotelChainServiceImpl implements HotelChainService {

    private final HotelChainRepository hotelChainRepository;
    private final AccountRepository accountRepository;
    private final HotelRepository hotelRepository;
    private final HotelChainMapper hotelChainMapper;
    private final HotelMapper hotelMapper;
    private final AccessGuard accessGuard;

    @Transactional
    @Override
    public ListHotelChainResponse create(HotelChainCreateRequest req) {
        accessGuard.requireSelfOrAdmin(req.getAccountId());

        Account account = accountRepository
                .findById(req.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found id=" + req.getAccountId()));

        if (account.getRole() != Role.HOTEL_OWNER) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Account must have role HOTEL_OWNER");
        }

        if (hotelChainRepository.findByAccount_Id(account.getId()) != null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Account already owns a hotel chain");
        }

        HotelChain hotelChain = hotelChainMapper.toEntity(req);
        hotelChain.setAccount(account);

        return hotelChainMapper.toResponse(hotelChainRepository.save(hotelChain));
    }

    @Override
    public ListHotelChainResponse update(Long id, HotelChainUpdateRequest req) {
        accessGuard.requireChainOwner(id);
        HotelChain hotelChain = hotelChainRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel chain not found with id=" + id));
        hotelChainMapper.updateEntity(hotelChain, req);
        return hotelChainMapper.toResponse(hotelChainRepository.save(hotelChain));
    }

    @Override
    public HotelChainDetailResponse getById(Long id) {
        accessGuard.requireChainOwner(id);
        HotelChain hotelChain = hotelChainRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel chain not found with id=" + id));

        List<Hotel> hotels = hotelRepository.findAllByHotelChain_Id(id);

        HotelChainDetailResponse response = hotelChainMapper.toDetailResponse(hotelChain);
        response.setHotels(hotels.stream().map(hotelMapper::toResponse).toList());
        return response;
    }

    @Override
    public PageResponse<ListHotelChainResponse> getAll(int page, int size) {
        accessGuard.requireAdmin();
        Pageable pageable = PageRequest.of(page, size, Sort.by("onCreate"));
        Page<ListHotelChainResponse> data =
                hotelChainRepository.findAll(pageable).map(hotelChainMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(Long id) {
        accessGuard.requireChainOwner(id);
        HotelChain hotelChain = hotelChainRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel chain not found with id=" + id));
        hotelChainRepository.delete(hotelChain);
    }
}
