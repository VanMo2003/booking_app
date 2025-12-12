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
public class AmenityResponse {
    Long id;
    String name;
    String description;
    boolean common;
    boolean active;
    Long hotelId;
    Date onCreate;
    Date onUpdate;
}
