package com.vanmo.booking_app_be.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelResponse {
    Long id;
    String name;
    String address;
    String phone;
    String description;
    String category;
    double rating;
    String pathImage;
    boolean active;
    String accountId;
    List<AmenityResponse> amenities;
    List<ServiceResponse> services;
    Date onCreate;
    Date onUpdate;
}
