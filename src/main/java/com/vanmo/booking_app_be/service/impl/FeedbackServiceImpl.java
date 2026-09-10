package com.vanmo.booking_app_be.service.impl;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.vanmo.booking_app_be.dto.request.FeedbackCreateRequest;
import com.vanmo.booking_app_be.dto.request.FeedbackUpdateRequest;
import com.vanmo.booking_app_be.dto.response.FeedbackResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.Feedback;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.FeedbackMapper;
import com.vanmo.booking_app_be.repository.FeedbackRepository;
import com.vanmo.booking_app_be.service.FeedbackService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackMapper feedbackMapper;

    @Override
    public FeedbackResponse create(FeedbackCreateRequest req) {
        Feedback entity = feedbackMapper.toEntity(req);
        return feedbackMapper.toResponse(feedbackRepository.save(entity));
    }

    @Override
    public FeedbackResponse update(Long id, FeedbackUpdateRequest req) {
        Feedback entity = feedbackRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found id=" + id));
        feedbackMapper.updateEntity(entity, req);
        return feedbackMapper.toResponse(feedbackRepository.save(entity));
    }

    @Override
    public FeedbackResponse getById(Long id) {
        Feedback entity = feedbackRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found id=" + id));
        return feedbackMapper.toResponse(entity);
    }

    @Override
    public PageResponse<FeedbackResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<FeedbackResponse> data = feedbackRepository.findAll(pageable).map(feedbackMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(Long id) {
        Feedback entity = feedbackRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found id=" + id));
        feedbackRepository.delete(entity);
    }
}
