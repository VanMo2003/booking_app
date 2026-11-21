package com.vanmo.booking_app_be.repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vanmo.booking_app_be.dto.response.ChatMessageRow;
import com.vanmo.booking_app_be.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversation_IdOrderByIdDesc(Long conversationId, Pageable pageable);

    List<ChatMessage> findByConversation_IdAndIdLessThanOrderByIdDesc(
            Long conversationId, Long beforeId, Pageable pageable);

    /** Messages of the branches' conversations written in [start, end), thread by thread in order, for reports. */
    @Query("select new com.vanmo.booking_app_be.dto.response.ChatMessageRow("
            + "m.conversation.id, m.senderSide, m.aiGenerated, m.onCreate) "
            + "from ChatMessage m "
            + "where m.conversation.hotel.id in :hotelIds and m.onCreate >= :start and m.onCreate < :end "
            + "order by m.conversation.id, m.id")
    List<ChatMessageRow> findForReport(
            @Param("hotelIds") Collection<Long> hotelIds, @Param("start") Date start, @Param("end") Date end);
}
