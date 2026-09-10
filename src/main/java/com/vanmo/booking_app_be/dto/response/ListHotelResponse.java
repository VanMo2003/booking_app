package com.vanmo.booking_app_be.dto.response;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vanmo.booking_app_be.entity.enums.HotelStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListHotelResponse {
    Long id;
    String name;
    String address;
    String phone;
    String description;
    String category;
    int rating;
    String pathImage;
    boolean active;
    HotelStatus status;
    String accountId;
    Long hotelChainId;
    List<AmenityResponse> amenities = new ArrayList<>();
    List<ServiceResponse> services = new ArrayList<>();
    Date onCreate;
    Date onUpdate;
}
