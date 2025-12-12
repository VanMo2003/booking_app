package com.vanmo.booking_app_be.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

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
    Date onCreate;
    Date onUpdate;
}
