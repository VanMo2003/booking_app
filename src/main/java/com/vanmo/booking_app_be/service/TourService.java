package com.vanmo.booking_app_be.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

import com.vanmo.booking_app_be.dto.request.TourCreateRequest;
import com.vanmo.booking_app_be.dto.request.TourUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.TourDestinationDTO;
import com.vanmo.booking_app_be.dto.response.TourHomeResponse;
import com.vanmo.booking_app_be.dto.response.TourResponse;
import com.vanmo.booking_app_be.entity.enums.TravelTheme;

public interface TourService {
    TourResponse create(TourCreateRequest req);

    TourResponse update(Long id, TourUpdateRequest req);

    TourResponse uploadImage(Long id, MultipartFile file);

    TourResponse getById(Long id);

    /** Running tours first, cheapest first. */
    List<TourResponse> getAllByHotel(Long hotelId);

    void delete(Long id);

    // ---- guests browsing tours outside a hotel page (public) ----

    PageResponse<TourResponse> search(String q, TravelTheme theme, BigDecimal maxPrice, int page, int size);

    List<TourDestinationDTO> destinations();

    TourHomeResponse home(Set<TravelTheme> interests);
}
