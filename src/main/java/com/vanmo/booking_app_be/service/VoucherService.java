package com.vanmo.booking_app_be.service;

import java.math.BigDecimal;
import java.util.List;

import com.vanmo.booking_app_be.dto.request.VoucherCheckRequest;
import com.vanmo.booking_app_be.dto.request.VoucherRequest;
import com.vanmo.booking_app_be.dto.response.VoucherCheckResponse;
import com.vanmo.booking_app_be.dto.response.VoucherResponse;
import com.vanmo.booking_app_be.entity.Customer;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Voucher;

public interface VoucherService {
    /** Owner for the chain or a branch of it; a manager for their own branch only. */
    VoucherResponse create(VoucherRequest req);

    VoucherResponse update(Long id, VoucherRequest req);

    void delete(Long id);

    /** The chain's vouchers, for its owner (or a branch's, for its manager when hotelId is given). */
    List<VoucherResponse> listForTeam(Long hotelChainId, Long hotelId);

    /** Public: usable today, at a branch (hotelId) or anywhere. */
    List<VoucherResponse> available(Long hotelId);

    VoucherCheckResponse check(VoucherCheckRequest req);

    /**
     * The same answer for a guest the caller already resolved, used by the chat assistant: auto-reply runs with
     * no signed-in account, so the once-per-guest limit would otherwise be read as "never used".
     */
    VoucherCheckResponse checkFor(VoucherCheckRequest req, Customer customer);

    /** The voucher an order will use, refused with TOUR_BOOKING_INVALID when it does not apply. */
    Voucher reserve(String code, Customer customer, Hotel hotel, BigDecimal tourAmount);

    /** A cancelled order gives its use back. */
    void release(Voucher voucher);
}
