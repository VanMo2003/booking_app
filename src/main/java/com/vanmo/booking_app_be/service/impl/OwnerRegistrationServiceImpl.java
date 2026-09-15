package com.vanmo.booking_app_be.service.impl;

import java.util.Date;

import jakarta.transaction.Transactional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.vanmo.booking_app_be.dto.request.AccountCreateRequest;
import com.vanmo.booking_app_be.dto.request.OwnerRegistrationRequest;
import com.vanmo.booking_app_be.dto.request.OwnerRejectRequest;
import com.vanmo.booking_app_be.dto.request.OwnerResubmitRequest;
import com.vanmo.booking_app_be.dto.response.ListHotelChainResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.HotelChain;
import com.vanmo.booking_app_be.entity.enums.ApprovalStatus;
import com.vanmo.booking_app_be.entity.enums.NotificationType;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.event.OwnerReviewedEvent;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.HotelChainMapper;
import com.vanmo.booking_app_be.repository.HotelChainRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.NotificationService;
import com.vanmo.booking_app_be.service.OwnerRegistrationService;
import com.vanmo.booking_app_be.utils.AppLinks;
import com.vanmo.booking_app_be.utils.NotificationMessages;

import lombok.RequiredArgsConstructor;

/**
 * Hotel owners sign themselves up. The account can log in straight away, but everything that runs the hotel is
 * closed to it ({@link AccessGuard#requireApprovedOwner()}, {@link AccessGuard#requireChainOwner(Long)}) until an
 * admin approves the registration.
 */
@Service
@RequiredArgsConstructor
public class OwnerRegistrationServiceImpl implements OwnerRegistrationService {

    private final HotelChainRepository hotelChainRepository;
    private final HotelChainMapper hotelChainMapper;
    private final AccountServiceImpl accountServiceImpl;
    private final AccessGuard accessGuard;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public ListHotelChainResponse register(OwnerRegistrationRequest req) {
        Account account = accountServiceImpl.createAccount(AccountCreateRequest.builder()
                .username(req.getUsername().trim())
                .password(req.getPassword())
                .role(Role.HOTEL_OWNER.name())
                .status(true)
                .build());

        HotelChain chain = HotelChain.builder().account(account).build();
        applyDetails(
                chain,
                req.getHotelName(),
                req.getDescription(),
                req.getOwnerName(),
                req.getEmail(),
                req.getPhone(),
                req.getAddress());
        chain.setApprovalStatus(ApprovalStatus.PENDING);
        chain.setSubmittedAt(new Date());
        chain = hotelChainRepository.save(chain);

        notifyAdmins(chain, false);
        return hotelChainMapper.toResponse(chain);
    }

    @Override
    public ListHotelChainResponse getMine() {
        return hotelChainMapper.toResponse(accessGuard.requireOwnChain());
    }

    @Transactional
    @Override
    public ListHotelChainResponse resubmit(OwnerResubmitRequest req) {
        HotelChain chain = accessGuard.requireOwnChain();
        if (chain.isApproved()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Registration has already been approved");
        }
        applyDetails(
                chain,
                req.getHotelName(),
                req.getDescription(),
                req.getOwnerName(),
                req.getEmail(),
                req.getPhone(),
                req.getAddress());
        boolean wasRejected = chain.getApprovalStatus() == ApprovalStatus.REJECTED;
        chain.setApprovalStatus(ApprovalStatus.PENDING);
        chain.setSubmittedAt(new Date());
        chain.setReviewedAt(null);
        chain = hotelChainRepository.save(chain);

        // Editing a still-pending submission doesn't need to ping admins again.
        if (wasRejected) {
            notifyAdmins(chain, true);
        }
        return hotelChainMapper.toResponse(chain);
    }

    @Override
    public PageResponse<ListHotelChainResponse> getAll(ApprovalStatus status, int page, int size) {
        accessGuard.requireAdmin();
        // The review queue is first come, first served; decided registrations show the latest decision first.
        Sort sort = status == ApprovalStatus.PENDING
                ? Sort.by(Sort.Direction.ASC, "submittedAt")
                : Sort.by(Sort.Direction.DESC, "reviewedAt").and(Sort.by(Sort.Direction.DESC, "onCreate"));
        PageRequest pageable = PageRequest.of(page, size, sort);
        Page<HotelChain> data = status == null
                ? hotelChainRepository.findAll(pageable)
                : hotelChainRepository.findAllByApprovalStatus(status, pageable);
        return PageResponse.from(data.map(hotelChainMapper::toResponse));
    }

    @Override
    public ListHotelChainResponse getById(Long id) {
        accessGuard.requireAdmin();
        return hotelChainMapper.toResponse(findChain(id));
    }

    @Transactional
    @Override
    public ListHotelChainResponse approve(Long id) {
        accessGuard.requireAdmin();
        HotelChain chain = findPending(id);
        chain.setApprovalStatus(ApprovalStatus.APPROVED);
        chain.setRejectionReason(null);
        chain.setReviewedAt(new Date());
        chain = hotelChainRepository.save(chain);

        notificationService.notify(
                chain.getAccount(),
                NotificationType.OWNER_APPROVED,
                NotificationMessages.ownerApproved(chain),
                AppLinks.ownerStatus(),
                chain.getId());
        eventPublisher.publishEvent(OwnerReviewedEvent.of(chain));
        return hotelChainMapper.toResponse(chain);
    }

    @Transactional
    @Override
    public ListHotelChainResponse reject(Long id, OwnerRejectRequest req) {
        accessGuard.requireAdmin();
        HotelChain chain = findPending(id);
        String reason = req.getReason().trim();
        chain.setApprovalStatus(ApprovalStatus.REJECTED);
        chain.setRejectionReason(reason);
        chain.setReviewedAt(new Date());
        chain = hotelChainRepository.save(chain);

        notificationService.notify(
                chain.getAccount(),
                NotificationType.OWNER_REJECTED,
                NotificationMessages.ownerRejected(chain, reason),
                AppLinks.ownerStatus(),
                chain.getId());
        eventPublisher.publishEvent(OwnerReviewedEvent.of(chain));
        return hotelChainMapper.toResponse(chain);
    }

    private void notifyAdmins(HotelChain chain, boolean resubmitted) {
        notificationService.notifyAdmins(
                NotificationType.OWNER_REGISTERED,
                NotificationMessages.ownerSubmitted(chain, resubmitted),
                AppLinks.ownerRegistration(chain.getId()),
                chain.getId());
    }

    private HotelChain findChain(Long id) {
        return hotelChainRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel chain not found with id=" + id));
    }

    private HotelChain findPending(Long id) {
        HotelChain chain = findChain(id);
        if (chain.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new AppException(ErrorCode.REGISTRATION_NOT_PENDING);
        }
        return chain;
    }

    private static void applyDetails(
            HotelChain chain,
            String hotelName,
            String description,
            String ownerName,
            String email,
            String phone,
            String address) {
        chain.setName(hotelName.trim());
        chain.setDescription(description == null || description.isBlank() ? null : description.trim());
        chain.setOwnerName(ownerName.trim());
        chain.setEmail(email.trim());
        chain.setPhone(phone.trim());
        chain.setAddress(address.trim());
    }
}
