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
public class HotelChainDetailResponse {
    Long id;
    String name;
    String description;
    String pathImage;
    String accountId;
    List<ListHotelResponse> hotels = new ArrayList<>();
    Date onCreate;
    Date onUpdate;
}
