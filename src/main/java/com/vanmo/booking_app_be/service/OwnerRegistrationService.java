package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.OwnerRegistrationRequest;
import com.vanmo.booking_app_be.dto.request.OwnerRejectRequest;
import com.vanmo.booking_app_be.dto.request.OwnerResubmitRequest;
import com.vanmo.booking_app_be.dto.response.ListHotelChainResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.enums.ApprovalStatus;

public interface OwnerRegistrationService {
    ListHotelChainResponse register(OwnerRegistrationRequest req);

    ListHotelChainResponse getMine();

    ListHotelChainResponse resubmit(OwnerResubmitRequest req);

    PageResponse<ListHotelChainResponse> getAll(ApprovalStatus status, int page, int size);

    ListHotelChainResponse getById(Long id);

    ListHotelChainResponse approve(Long id);

    ListHotelChainResponse reject(Long id, OwnerRejectRequest req);
}
