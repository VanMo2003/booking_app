package com.vanmo.booking_app_be.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.vanmo.booking_app_be.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findAllByAccount_Id(String accountId, Pageable pageable);

    long countByAccount_IdAndReadFalse(String accountId);

    Optional<Notification> findByIdAndAccount_Id(Long id, String accountId);

    @Transactional
    @Modifying
    @Query("update Notification n set n.read = true where n.account.id = :accountId and n.read = false")
    int markAllRead(@Param("accountId") String accountId);

    @Transactional
    @Modifying
    @Query("delete from Notification n where n.account.id = :accountId")
    int deleteAllByAccountId(@Param("accountId") String accountId);
}
