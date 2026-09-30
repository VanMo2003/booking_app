package com.vanmo.booking_app_be.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.ChatMessageRequest;
import com.vanmo.booking_app_be.dto.request.ConversationCreateRequest;
import com.vanmo.booking_app_be.dto.response.ChatMessageResponse;
import com.vanmo.booking_app_be.dto.response.ChatUnreadResponse;
import com.vanmo.booking_app_be.dto.response.ConversationResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.service.ChatService;

import lombok.RequiredArgsConstructor;

/**
 * Guest ↔ branch chat over REST. Real-time delivery of the same data goes out on the {@code /ws/chat} WebSocket
 * (see {@code ChatWebSocketHandler}); clients send through here and listen there.
 */
@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** Customer only: returns the existing conversation with that branch, or starts one. */
    @PostMapping
    public ApiResponse<ConversationResponse> open(@Valid @RequestBody ConversationCreateRequest req) {
        return ApiResponse.ok(chatService.open(req));
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<ConversationResponse>> getMine(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(chatService.getMine(page, size));
    }

    /** Owner, manager or staff; without {@code hotelId} it spans every branch the caller answers for. */
    @GetMapping("/inbox")
    public ApiResponse<PageResponse<ConversationResponse>> getInbox(
            @RequestParam(required = false) Long hotelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(chatService.getInbox(hotelId, page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<ChatUnreadResponse> countUnread(@RequestParam(required = false) Long hotelId) {
        return ApiResponse.ok(chatService.countUnread(hotelId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ConversationResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(chatService.getById(id));
    }

    /** Newest first. Page back with {@code beforeId} = the oldest message id already loaded. */
    @GetMapping("/{id}/messages")
    public ApiResponse<List<ChatMessageResponse>> getMessages(
            @PathVariable Long id,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "30") int size) {
        return ApiResponse.ok(chatService.getMessages(id, beforeId, size));
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<ChatMessageResponse> send(@PathVariable Long id, @Valid @RequestBody ChatMessageRequest req) {
        return ApiResponse.ok(chatService.send(id, req));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<ConversationResponse> markRead(@PathVariable Long id) {
        return ApiResponse.ok(chatService.markRead(id));
    }
}
