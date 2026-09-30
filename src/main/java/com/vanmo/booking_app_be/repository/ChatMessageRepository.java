package com.vanmo.booking_app_be.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.vanmo.booking_app_be.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversation_IdOrderByIdDesc(Long conversationId, Pageable pageable);

    List<ChatMessage> findByConversation_IdAndIdLessThanOrderByIdDesc(
            Long conversationId, Long beforeId, Pageable pageable);
}
