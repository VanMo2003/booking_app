package com.vanmo.booking_app_be.dto.response;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HotelDetailResponse {
    Long id;
    String name;
    String address;
    String phone;
    String description;
    String category;
    int rating;
    String pathImage;
    boolean active;
    String accountId;
    Long hotelChainId;
    List<String> images = new ArrayList<>();
    List<RoomResponse> rooms = new ArrayList<>();
    List<AmenityResponse> amenities = new ArrayList<>();
    List<ServiceResponse> services = new ArrayList<>();
    Date onCreate;
    Date onUpdate;
}
