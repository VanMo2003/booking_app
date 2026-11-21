package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.FeedbackCreateRequest;
import com.vanmo.booking_app_be.dto.request.FeedbackReplyRequest;
import com.vanmo.booking_app_be.dto.request.FeedbackUpdateRequest;
import com.vanmo.booking_app_be.dto.response.FeedbackResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

public interface FeedbackService {
    FeedbackResponse create(FeedbackCreateRequest req);

    FeedbackResponse update(Long id, FeedbackUpdateRequest req);

    FeedbackResponse getById(Long id);

    PageResponse<FeedbackResponse> getAll(int page, int size);

    /** One branch's reviews, newest first - public, like the rest of a branch's page. */
    PageResponse<FeedbackResponse> getByHotel(Long hotelId, int page, int size);

    /** The branch answers a review publicly. Owner or branch manager only; sending it again replaces the answer. */
    FeedbackResponse reply(Long id, FeedbackReplyRequest req);

    void delete(Long id);
}
