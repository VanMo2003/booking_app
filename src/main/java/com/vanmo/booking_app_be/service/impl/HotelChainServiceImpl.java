package com.vanmo.booking_app_be.service.impl;

import java.util.Date;
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
import com.vanmo.booking_app_be.entity.enums.ApprovalStatus;
import com.vanmo.booking_app_be.entity.enums.NotificationType;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.HotelChainMapper;
import com.vanmo.booking_app_be.mapper.HotelMapper;
import com.vanmo.booking_app_be.repository.HotelChainRepository;
import com.vanmo.booking_app_be.repository.HotelRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.HotelChainService;
import com.vanmo.booking_app_be.service.NotificationService;
import com.vanmo.booking_app_be.utils.AppLinks;
import com.vanmo.booking_app_be.utils.NotificationMessages;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HotelChainServiceImpl implements HotelChainService {

    private final HotelChainRepository hotelChainRepository;
    private final HotelRepository hotelRepository;
    private final HotelChainMapper hotelChainMapper;
    private final HotelMapper hotelMapper;
    private final AccessGuard accessGuard;
    private final NotificationService notificationService;

    /**
     * An owner without a chain (e.g. after deleting theirs) submits a new one. Like a fresh owner registration it
     * waits for an admin's review; admins themselves can't open chains.
     */
    @Transactional
    @Override
    public ListHotelChainResponse create(HotelChainCreateRequest req) {
        Account current = accessGuard.currentAccount();
        if (current.getRole() != Role.HOTEL_OWNER || !current.getId().equals(req.getAccountId())) {
            throw new AppException(ErrorCode.UN_AUTHORIZED);
        }

        if (hotelChainRepository.findByAccount_Id(current.getId()) != null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Account already owns a hotel chain");
        }

        HotelChain hotelChain = hotelChainMapper.toEntity(req);
        hotelChain.setAccount(current);
        hotelChain.setApprovalStatus(ApprovalStatus.PENDING);
        hotelChain.setSubmittedAt(new Date());
        HotelChain saved = hotelChainRepository.save(hotelChain);

        notificationService.notifyAdmins(
                NotificationType.OWNER_REGISTERED,
                NotificationMessages.ownerSubmitted(saved, false),
                AppLinks.ownerRegistration(saved.getId()),
                saved.getId());
        return hotelChainMapper.toResponse(saved);
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
