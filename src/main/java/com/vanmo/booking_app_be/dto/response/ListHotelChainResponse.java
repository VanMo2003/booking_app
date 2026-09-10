package com.vanmo.booking_app_be.dto.response;

import java.util.Date;

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
public class ListHotelChainResponse {
    Long id;
    String name;
    String description;
    String pathImage;
    String accountId;
    Date onCreate;
    Date onUpdate;
}
