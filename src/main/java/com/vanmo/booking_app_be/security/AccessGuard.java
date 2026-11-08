package com.vanmo.booking_app_be.security;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Employee;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.HotelChain;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.repository.AccountRepository;
import com.vanmo.booking_app_be.repository.EmployeeRepository;
import com.vanmo.booking_app_be.repository.HotelChainRepository;
import com.vanmo.booking_app_be.repository.HotelRepository;

import lombok.RequiredArgsConstructor;

/**
 * Central place for "is the current account allowed to touch this chain/branch" checks.
 * Ownership checks need DB joins (chain -> owner, branch -> manager, staff -> their one branch),
 * so they live here as reusable service-layer calls rather than as SpEL on each controller method.
 */
@Component
@RequiredArgsConstructor
public class AccessGuard {

    private final AccountRepository accountRepository;
    private final HotelRepository hotelRepository;
    private final HotelChainRepository hotelChainRepository;
    private final EmployeeRepository employeeRepository;

    public Account currentAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UN_AUTHENTICATED);
        }
        return accountRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.UN_AUTHENTICATED));
    }

    /** The signed-in account, or null for an anonymous caller (public endpoints that answer more when signed in). */
    public Account currentAccountOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return null;
        }
        return accountRepository.findByUsername(authentication.getName()).orElse(null);
    }

    public void requireAdmin() {
        if (currentAccount().getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.UN_AUTHORIZED);
        }
    }

    /**
     * Role-only "works here" check for resources that aren't hotel-scoped (e.g. Customer profiles,
     * which are global). Use {@link #requireBranchStaffOrAbove(Long)} whenever a hotel id is
     * available - that one also verifies the caller actually belongs to that branch.
     */
    public Account requireStaffOrAbove() {
        Account current = currentAccount();
        if (current.getRole() == Role.CUSTOMER) {
            throw new AppException(ErrorCode.UN_AUTHORIZED);
        }
        return current;
    }

    public void requireSelfOrAdmin(String accountId) {
        Account current = currentAccount();
        if (current.getRole() != Role.ADMIN && !current.getId().equals(accountId)) {
            throw new AppException(ErrorCode.UN_AUTHORIZED);
        }
    }

    /**
     * The calling HOTEL_OWNER's own chain in whatever review state it is. Only the registration endpoints use this -
     * a pending or rejected owner may still read their status and resubmit, nothing else.
     */
    public HotelChain requireOwnChain() {
        Account current = currentAccount();
        if (current.getRole() != Role.HOTEL_OWNER) {
            throw new AppException(ErrorCode.UN_AUTHORIZED);
        }
        HotelChain chain = hotelChainRepository.findByAccount_Id(current.getId());
        if (chain == null) {
            throw new ResourceNotFoundException("Hotel chain not found for this account");
        }
        return chain;
    }

    /** A HOTEL_OWNER whose registration an admin has approved - the only owners allowed to run their hotel. */
    public HotelChain requireApprovedOwner() {
        HotelChain chain = requireOwnChain();
        if (!chain.isApproved()) {
            throw new AppException(ErrorCode.OWNER_NOT_APPROVED);
        }
        return chain;
    }

    public HotelChain requireChainOwner(Long hotelChainId) {
        Account current = currentAccount();
        HotelChain chain = hotelChainRepository
                .findById(hotelChainId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel chain not found with id=" + hotelChainId));

        if (current.getRole() == Role.ADMIN) {
            return chain;
        }
        if (current.getRole() == Role.HOTEL_OWNER && chain.getAccount().getId().equals(current.getId())) {
            if (!chain.isApproved()) {
                throw new AppException(ErrorCode.OWNER_NOT_APPROVED);
            }
            return chain;
        }
        throw new AppException(ErrorCode.UN_AUTHORIZED);
    }

    public Hotel requireBranchManagerOrAbove(Long hotelId) {
        return branchManagerOrAbove(currentAccount(), hotelId);
    }

    public Hotel requireBranchStaffOrAbove(Long hotelId) {
        return branchStaffOrAbove(currentAccount(), hotelId);
    }

    /**
     * Write access to a branch's own catalogue and people (rooms, amenities, services, staff accounts). Same branch
     * check as {@link #requireBranchStaffOrAbove(Long)}, but ADMIN is refused: admins review this data, they don't
     * author it.
     */
    public Hotel requireBranchEditor(Long hotelId) {
        Account current = currentAccount();
        rejectAdmin(current);
        return branchStaffOrAbove(current, hotelId);
    }

    /** Manager-level write access (deletes, staff accounts) with ADMIN refused - see {@link #requireBranchEditor}. */
    public Hotel requireBranchManagerEditor(Long hotelId) {
        Account current = currentAccount();
        rejectAdmin(current);
        return branchManagerOrAbove(current, hotelId);
    }

    public List<Long> hotelIdsForChain(Long hotelChainId) {
        return hotelRepository.findAllByHotelChain_Id(hotelChainId).stream()
                .map(Hotel::getId)
                .toList();
    }

    /**
     * The branch's own team - its chain owner, its manager or one of its staff - who share the branch's chat inbox.
     * Admins are refused as well: conversations are private between the guest and the hotel.
     */
    public Hotel requireBranchTeam(Long hotelId) {
        Account current = currentAccount();
        if (current.getRole() == Role.ADMIN || current.getRole() == Role.CUSTOMER) {
            throw new AppException(ErrorCode.UN_AUTHORIZED);
        }
        return branchStaffOrAbove(current, hotelId);
    }

    /** Owner of the chain, manager of the branch and its staff: the people who answer for the branch. */
    public List<Account> branchTeam(Hotel hotel) {
        Map<String, Account> team = new LinkedHashMap<>();
        Account owner = hotel.getHotelChain().getAccount();
        team.put(owner.getId(), owner);
        if (hotel.getAccount() != null) {
            team.putIfAbsent(hotel.getAccount().getId(), hotel.getAccount());
        }
        for (Employee employee : employeeRepository.findAllByHotelId(hotel.getId())) {
            team.putIfAbsent(employee.getAccount().getId(), employee.getAccount());
        }
        return new ArrayList<>(team.values());
    }

    /** Branches whose chat this account answers: an owner's whole chain, a manager's branches, a staff member's one. */
    public List<Long> branchIdsServedBy(Account account) {
        return switch (account.getRole()) {
            case HOTEL_OWNER -> {
                HotelChain chain = hotelChainRepository.findByAccount_Id(account.getId());
                yield chain == null ? List.of() : hotelIdsForChain(chain.getId());
            }
            case HOTEL_MANAGER -> hotelRepository.findAllByAccount_Id(account.getId()).stream()
                    .map(Hotel::getId)
                    .toList();
            case STAFF -> {
                Employee employee = employeeRepository.findByAccount_Id(account.getId());
                yield employee == null ? List.of() : List.of(employee.getHotel().getId());
            }
            default -> throw new AppException(ErrorCode.UN_AUTHORIZED);
        };
    }

    private void rejectAdmin(Account current) {
        if (current.getRole() == Role.ADMIN) {
            throw new AppException(ErrorCode.ADMIN_READ_ONLY);
        }
    }

    private Hotel branchManagerOrAbove(Account current, Long hotelId) {
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id=" + hotelId));

        boolean allowed =
                switch (current.getRole()) {
                    case ADMIN -> true;
                    case HOTEL_OWNER -> hotel.getHotelChain()
                            .getAccount()
                            .getId()
                            .equals(current.getId());
                    case HOTEL_MANAGER -> hotel.getAccount().getId().equals(current.getId());
                    default -> false;
                };

        if (!allowed) {
            throw new AppException(ErrorCode.UN_AUTHORIZED);
        }
        return hotel;
    }

    private Hotel branchStaffOrAbove(Account current, Long hotelId) {
        if (current.getRole() != Role.STAFF) {
            return branchManagerOrAbove(current, hotelId);
        }

        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id=" + hotelId));

        Employee employee = employeeRepository.findByAccount_Id(current.getId());
        if (employee == null || !employee.getHotel().getId().equals(hotelId)) {
            throw new AppException(ErrorCode.UN_AUTHORIZED);
        }
        return hotel;
    }
}
