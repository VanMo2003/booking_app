package com.vanmo.booking_app_be.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vanmo.booking_app_be.entity.Voucher;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Voucher> findAllByHotelChain_IdOrderByIdDesc(Long hotelChainId);

    /** Vouchers a guest can use today at a branch: active, in their dates, not used up, for its chain or for it. */
    @Query("select v from Voucher v where v.active = true and v.startsAt <= :today and v.endsAt >= :today "
            + "and (v.usageLimit is null or v.usedCount < v.usageLimit) "
            + "and v.hotelChain.id = :chainId and (v.hotel is null or v.hotel.id = :hotelId) order by v.endsAt")
    List<Voucher> findUsable(
            @Param("chainId") Long chainId, @Param("hotelId") Long hotelId, @Param("today") LocalDate today);

    /** Every voucher usable today anywhere, for the guests' voucher list. */
    @Query("select v from Voucher v where v.active = true and v.startsAt <= :today and v.endsAt >= :today "
            + "and (v.usageLimit is null or v.usedCount < v.usageLimit) order by v.endsAt")
    List<Voucher> findAllUsable(@Param("today") LocalDate today);
}
