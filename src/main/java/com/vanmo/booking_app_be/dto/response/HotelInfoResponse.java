package com.vanmo.booking_app_be.dto.response;

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
public class HotelInfoResponse {
    Long id;
    String name;
    String address;
    String phone;
    String description;
    String category;
    int rating;
    String pathImage;
    boolean active;
}
