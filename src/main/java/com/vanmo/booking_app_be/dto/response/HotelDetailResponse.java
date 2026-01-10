package com.vanmo.booking_app_be.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
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
    List<String> images = new ArrayList<>();
    List<RoomResponse> rooms = new ArrayList<>();
    List<AmenityResponse> amenities = new ArrayList<>();
    List<ServiceResponse> services = new ArrayList<>();
    Date onCreate;
    Date onUpdate;
}
