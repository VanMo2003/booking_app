package com.vanmo.booking_app_be.service;

import java.util.List;

import com.vanmo.booking_app_be.dto.request.ChatMessageRequest;
import com.vanmo.booking_app_be.dto.request.ConversationCreateRequest;
import com.vanmo.booking_app_be.dto.response.ChatMessageResponse;
import com.vanmo.booking_app_be.dto.response.ChatUnreadResponse;
import com.vanmo.booking_app_be.dto.response.ConversationResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

public interface ChatService {
    /** Customer opens (or reopens) their conversation with a branch. */
    ConversationResponse open(ConversationCreateRequest req);

    PageResponse<ConversationResponse> getMine(int page, int size);

    /** Branch team inbox; {@code hotelId} null = every branch the caller answers for. */
    PageResponse<ConversationResponse> getInbox(Long hotelId, int page, int size);

    ConversationResponse getById(Long id);

    /** Newest first; pass the oldest id already shown as {@code beforeId} to page back. */
    List<ChatMessageResponse> getMessages(Long conversationId, Long beforeId, int size);

    ChatMessageResponse send(Long conversationId, ChatMessageRequest req);

    ConversationResponse markRead(Long conversationId);

    /**
     * The chat assistant answers on the hotel's behalf. Internal (no caller); skipped and returns null when the thread
     * moved on past {@code replyToMessageId} while the answer was being generated.
     */
    ChatMessageResponse postAssistantReply(
            Long conversationId, Long replyToMessageId, String content, boolean handoff, String handoffReason);

    ChatUnreadResponse countUnread(Long hotelId);
}
