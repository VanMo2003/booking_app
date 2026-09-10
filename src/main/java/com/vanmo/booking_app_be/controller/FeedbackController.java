package com.vanmo.booking_app_be.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.FeedbackCreateRequest;
import com.vanmo.booking_app_be.dto.request.FeedbackUpdateRequest;
import com.vanmo.booking_app_be.dto.response.FeedbackResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.service.FeedbackService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ApiResponse<FeedbackResponse> create(@Valid @RequestBody FeedbackCreateRequest req) {
        return ApiResponse.ok(feedbackService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<FeedbackResponse> update(@PathVariable Long id, @Valid @RequestBody FeedbackUpdateRequest req) {
        return ApiResponse.ok(feedbackService.update(id, req));
    }

    @GetMapping("/{id}")
    public ApiResponse<FeedbackResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(feedbackService.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<FeedbackResponse>> getAll(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(feedbackService.getAll(page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        feedbackService.delete(id);
        return ApiResponse.ok(null);
    }
}
