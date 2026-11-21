package com.vanmo.booking_app_be.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vanmo.booking_app_be.entity.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByCustomer_IdAndHotel_Id(Long customerId, Long hotelId);

    /** A thread opened but never written in doesn't show up in any inbox. */
    Page<Conversation> findAllByCustomer_IdAndLastMessageAtIsNotNull(Long customerId, Pageable pageable);

    Page<Conversation> findAllByHotel_IdInAndLastMessageAtIsNotNull(Collection<Long> hotelIds, Pageable pageable);

    @Query("SELECT COALESCE(SUM(c.customerUnread), 0) FROM Conversation c WHERE c.customer.id = :customerId")
    long sumCustomerUnread(@Param("customerId") Long customerId);

    @Query("SELECT COALESCE(SUM(c.hotelUnread), 0) FROM Conversation c WHERE c.hotel.id IN :hotelIds")
    long sumHotelUnread(@Param("hotelIds") Collection<Long> hotelIds);

    /** Threads the assistant handed over that no one on the team has answered yet. */
    long countByHotel_IdInAndNeedsStaffTrue(Collection<Long> hotelIds);
}
