package com.vanmo.booking_app_be.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.vanmo.booking_app_be.entity.DeviceToken;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    Optional<DeviceToken> findByToken(String token);

    Optional<DeviceToken> findByTokenAndAccount_Id(String token, String accountId);

    @Query("select d.token from DeviceToken d where d.account.id in :accountIds")
    List<String> findTokensByAccountIds(@Param("accountIds") Collection<String> accountIds);

    @Transactional
    @Modifying
    @Query("delete from DeviceToken d where d.token in :tokens")
    int deleteAllByTokens(@Param("tokens") Collection<String> tokens);

    @Transactional
    @Modifying
    @Query("delete from DeviceToken d where d.account.id = :accountId")
    int deleteAllByAccountId(@Param("accountId") String accountId);
}
