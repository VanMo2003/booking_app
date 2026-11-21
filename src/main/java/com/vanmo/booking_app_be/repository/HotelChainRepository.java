package com.vanmo.booking_app_be.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.vanmo.booking_app_be.entity.HotelChain;
import com.vanmo.booking_app_be.entity.enums.ApprovalStatus;

public interface HotelChainRepository extends JpaRepository<HotelChain, Long> {
    HotelChain findByAccount_Id(String accountId);

    List<HotelChain> findAllByApprovalStatus(ApprovalStatus approvalStatus);

    Page<HotelChain> findAllByApprovalStatus(ApprovalStatus approvalStatus, Pageable pageable);

    @Transactional
    @Modifying
    @Query("update HotelChain c set c.approvalStatus = com.vanmo.booking_app_be.entity.enums.ApprovalStatus.APPROVED"
            + " where c.approvalStatus is null")
    int approveLegacyChains();
}
