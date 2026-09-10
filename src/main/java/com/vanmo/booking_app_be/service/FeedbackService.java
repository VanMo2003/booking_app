package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.FeedbackCreateRequest;
import com.vanmo.booking_app_be.dto.request.FeedbackUpdateRequest;
import com.vanmo.booking_app_be.dto.response.FeedbackResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

public interface FeedbackService {
    FeedbackResponse create(FeedbackCreateRequest req);

    FeedbackResponse update(Long id, FeedbackUpdateRequest req);

    FeedbackResponse getById(Long id);

    PageResponse<FeedbackResponse> getAll(int page, int size);

    void delete(Long id);
}
