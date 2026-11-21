package com.vanmo.booking_app_be.service.impl;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vanmo.booking_app_be.dto.request.VoucherCheckRequest;
import com.vanmo.booking_app_be.dto.request.VoucherRequest;
import com.vanmo.booking_app_be.dto.response.VoucherCheckResponse;
import com.vanmo.booking_app_be.dto.response.VoucherResponse;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Customer;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.HotelChain;
import com.vanmo.booking_app_be.entity.Tour;
import com.vanmo.booking_app_be.entity.Voucher;
import com.vanmo.booking_app_be.entity.enums.DiscountType;
import com.vanmo.booking_app_be.entity.enums.NotificationType;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.entity.enums.TourBookingStatus;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.repository.CustomerRepository;
import com.vanmo.booking_app_be.repository.HotelChainRepository;
import com.vanmo.booking_app_be.repository.HotelRepository;
import com.vanmo.booking_app_be.repository.TourBookingRepository;
import com.vanmo.booking_app_be.repository.TourRepository;
import com.vanmo.booking_app_be.repository.VoucherRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.NotificationService;
import com.vanmo.booking_app_be.service.VoucherService;
import com.vanmo.booking_app_be.utils.NotificationMessages;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {
    private static final EnumSet<TourBookingStatus> COUNTED =
            EnumSet.of(TourBookingStatus.PENDING, TourBookingStatus.CONFIRMED, TourBookingStatus.COMPLETED);

    private final VoucherRepository voucherRepository;
    private final HotelChainRepository hotelChainRepository;
    private final HotelRepository hotelRepository;
    private final TourRepository tourRepository;
    private final TourBookingRepository tourBookingRepository;
    private final CustomerRepository customerRepository;
    private final NotificationService notificationService;
    private final AccessGuard accessGuard;

    @Override
    @Transactional
    public VoucherResponse create(VoucherRequest req) {
        if (req.getCode() == null
                || req.getTitle() == null
                || req.getDiscountType() == null
                || req.getValue() == null
                || req.getStartsAt() == null
                || req.getEndsAt() == null) {
            throw bad("Cần đủ mã, tiêu đề, loại giảm, mức giảm và ngày bắt đầu/kết thúc.");
        }
        Hotel hotel = req.getHotelId() == null ? null : findHotel(req.getHotelId());
        HotelChain chain = hotel != null ? hotel.getHotelChain() : findChain(req.getHotelChainId());
        requireWriter(chain, hotel);
        String code = req.getCode().strip().toUpperCase(Locale.ROOT);
        if (voucherRepository.existsByCodeIgnoreCase(code)) {
            throw bad("Mã " + code + " đã có, hãy chọn mã khác.");
        }
        Voucher voucher =
                Voucher.builder().code(code).hotelChain(chain).hotel(hotel).build();
        apply(voucher, req);
        voucher = voucherRepository.save(voucher);
        announce(voucher);
        return toResponse(voucher);
    }

    @Override
    @Transactional
    public VoucherResponse update(Long id, VoucherRequest req) {
        Voucher voucher = find(id);
        requireWriter(voucher.getHotelChain(), voucher.getHotel());
        apply(voucher, req);
        return toResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Voucher voucher = find(id);
        requireWriter(voucher.getHotelChain(), voucher.getHotel());
        voucherRepository.delete(voucher);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> listForTeam(Long hotelChainId, Long hotelId) {
        if (hotelId != null) {
            Hotel hotel = accessGuard.requireBranchManagerOrAbove(hotelId);
            return voucherRepository
                    .findAllByHotelChain_IdOrderByIdDesc(hotel.getHotelChain().getId())
                    .stream()
                    .filter(v -> v.getHotel() == null || v.getHotel().getId().equals(hotelId))
                    .map(this::toResponse)
                    .toList();
        }
        accessGuard.requireChainOwner(hotelChainId);
        return voucherRepository.findAllByHotelChain_IdOrderByIdDesc(hotelChainId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> available(Long hotelId) {
        if (hotelId == null) {
            return voucherRepository.findAllUsable(TourPricing.today()).stream()
                    .map(this::toResponse)
                    .toList();
        }
        Hotel hotel = findHotel(hotelId);
        return voucherRepository.findUsable(hotel.getHotelChain().getId(), hotelId, TourPricing.today()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherCheckResponse check(VoucherCheckRequest req) {
        return answer(
                req,
                usedBy(voucherRepository
                        .findByCodeIgnoreCase(normalize(req.getCode()))
                        .orElse(null)));
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherCheckResponse checkFor(VoucherCheckRequest req, Customer customer) {
        Voucher voucher =
                voucherRepository.findByCodeIgnoreCase(normalize(req.getCode())).orElse(null);
        long used = voucher == null || customer == null
                ? 0
                : tourBookingRepository.countByCustomer_IdAndVoucher_IdAndStatusIn(
                        customer.getId(), voucher.getId(), COUNTED);
        return answer(req, used);
    }

    private static String normalize(String code) {
        return code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
    }

    private VoucherCheckResponse answer(VoucherCheckRequest req, long usedByGuest) {
        Tour tour = tourRepository
                .findById(req.getTourId())
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found id=" + req.getTourId()));
        int children = req.getChildren() == null ? 0 : req.getChildren();
        BigDecimal amount = tourAmount(tour, req.getGuests() - children, children);
        String code = normalize(req.getCode());
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code).orElse(null);
        if (voucher == null) {
            return VoucherCheckResponse.builder()
                    .valid(false)
                    .code(code)
                    .message("Không có mã " + code + ".")
                    .tourAmount(amount)
                    .discount(BigDecimal.ZERO)
                    .finalAmount(amount)
                    .build();
        }
        Hotel hotel = tour.getHotel();
        String refusal = VoucherRules.refusal(
                voucher, hotel.getHotelChain().getId(), hotel.getId(), amount, TourPricing.today(), usedByGuest);
        BigDecimal discount = refusal == null ? VoucherRules.discount(voucher, amount) : BigDecimal.ZERO;
        return VoucherCheckResponse.builder()
                .valid(refusal == null)
                .code(voucher.getCode())
                .message(refusal == null ? voucher.getTitle() : refusal)
                .tourAmount(amount)
                .discount(discount)
                .finalAmount(amount.subtract(discount))
                .build();
    }

    @Override
    public Voucher reserve(String code, Customer customer, Hotel hotel, BigDecimal tourAmount) {
        String normalized = code.strip().toUpperCase(Locale.ROOT);
        Voucher voucher = voucherRepository
                .findByCodeIgnoreCase(normalized)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_BOOKING_INVALID, "Không có mã " + normalized + "."));
        long used = tourBookingRepository.countByCustomer_IdAndVoucher_IdAndStatusIn(
                customer.getId(), voucher.getId(), COUNTED);
        String refusal = VoucherRules.refusal(
                voucher, hotel.getHotelChain().getId(), hotel.getId(), tourAmount, TourPricing.today(), used);
        if (refusal != null) {
            throw new AppException(ErrorCode.TOUR_BOOKING_INVALID, refusal);
        }
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        return voucherRepository.save(voucher);
    }

    @Override
    public void release(Voucher voucher) {
        if (voucher != null && voucher.getUsedCount() > 0) {
            voucher.setUsedCount(voucher.getUsedCount() - 1);
            voucherRepository.save(voucher);
        }
    }

    /** Adults at the day's adult price plus children at the child price, promotion included. */
    static BigDecimal tourAmount(Tour tour, int adults, int children) {
        TourPricing.Price price = TourPricing.of(tour, TourPricing.today());
        return price.adult()
                .multiply(BigDecimal.valueOf(Math.max(adults, 0)))
                .add(price.child().multiply(BigDecimal.valueOf(Math.max(children, 0))));
    }

    /** Uses of the voucher by the calling guest; 0 for anyone else (a check is only advice). */
    private long usedBy(Voucher voucher) {
        Account caller = accessGuard.currentAccountOrNull();
        if (caller == null || caller.getRole() != Role.CUSTOMER) {
            return 0;
        }
        Customer customer = customerRepository.findByAccount_Id(caller.getId());
        return customer == null
                ? 0
                : tourBookingRepository.countByCustomer_IdAndVoucher_IdAndStatusIn(
                        customer.getId(), voucher.getId(), COUNTED);
    }

    private void apply(Voucher voucher, VoucherRequest req) {
        if (req.getTitle() != null) {
            voucher.setTitle(req.getTitle().strip());
        }
        if (req.getDescription() != null) {
            voucher.setDescription(
                    req.getDescription().isBlank() ? null : req.getDescription().strip());
        }
        if (req.getDiscountType() != null) {
            voucher.setDiscountType(req.getDiscountType());
        }
        if (req.getValue() != null) {
            voucher.setValue(req.getValue());
        }
        if (req.getMaxDiscount() != null) {
            voucher.setMaxDiscount(req.getMaxDiscount().signum() == 0 ? null : req.getMaxDiscount());
        }
        if (req.getMinOrderAmount() != null) {
            voucher.setMinOrderAmount(req.getMinOrderAmount().signum() == 0 ? null : req.getMinOrderAmount());
        }
        if (req.getStartsAt() != null) {
            voucher.setStartsAt(req.getStartsAt());
        }
        if (req.getEndsAt() != null) {
            voucher.setEndsAt(req.getEndsAt());
        }
        if (req.getUsageLimit() != null) {
            voucher.setUsageLimit(req.getUsageLimit() == 0 ? null : req.getUsageLimit());
        }
        if (req.getPerCustomerLimit() != null) {
            voucher.setPerCustomerLimit(req.getPerCustomerLimit());
        }
        if (req.getActive() != null) {
            voucher.setActive(req.getActive());
        }
        if (voucher.getDiscountType() == DiscountType.PERCENT
                && voucher.getValue().compareTo(BigDecimal.valueOf(90)) > 0) {
            throw bad("Giảm theo phần trăm tối đa 90%.");
        }
        if (voucher.getEndsAt().isBefore(voucher.getStartsAt())) {
            throw bad("Ngày kết thúc phải sau ngày bắt đầu.");
        }
    }

    /** Owner of the chain, or the manager of the branch the voucher is for. */
    private void requireWriter(HotelChain chain, Hotel hotel) {
        if (hotel != null) {
            accessGuard.requireBranchManagerEditor(hotel.getId());
            return;
        }
        accessGuard.requireChainOwner(chain.getId());
    }

    /** Guests who ordered at the chain before hear about a new voucher that already runs. */
    private void announce(Voucher voucher) {
        if (!voucher.isActive() || voucher.getStartsAt().isAfter(TourPricing.today())) {
            return;
        }
        List<Account> guests = tourBookingRepository.findCustomerAccountsOfChain(
                voucher.getHotelChain().getId());
        if (!guests.isEmpty()) {
            notificationService.notifyAccounts(
                    guests,
                    NotificationType.PROMOTION,
                    NotificationMessages.voucherLaunched(
                            voucher.getHotelChain().getName(),
                            voucher.getCode(),
                            voucher.getTitle(),
                            voucher.getEndsAt()),
                    null,
                    voucher.getId());
        }
    }

    VoucherResponse toResponse(Voucher voucher) {
        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .title(voucher.getTitle())
                .description(voucher.getDescription())
                .discountType(voucher.getDiscountType())
                .value(voucher.getValue())
                .maxDiscount(voucher.getMaxDiscount())
                .minOrderAmount(voucher.getMinOrderAmount())
                .startsAt(voucher.getStartsAt())
                .endsAt(voucher.getEndsAt())
                .usageLimit(voucher.getUsageLimit())
                .usedCount(voucher.getUsedCount())
                .perCustomerLimit(voucher.getPerCustomerLimit())
                .active(voucher.isActive())
                .hotelChainId(voucher.getHotelChain().getId())
                .hotelChainName(voucher.getHotelChain().getName())
                .hotelId(voucher.getHotel() == null ? null : voucher.getHotel().getId())
                .hotelName(
                        voucher.getHotel() == null ? null : voucher.getHotel().getName())
                .build();
    }

    private Voucher find(Long id) {
        return voucherRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found id=" + id));
    }

    private Hotel findHotel(Long id) {
        return hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found id=" + id));
    }

    private HotelChain findChain(Long id) {
        if (id == null) {
            throw bad("Cần hotelChainId hoặc hotelId.");
        }
        return hotelChainRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel chain not found id=" + id));
    }

    private static AppException bad(String message) {
        return new AppException(ErrorCode.BAD_REQUEST, message);
    }
}
