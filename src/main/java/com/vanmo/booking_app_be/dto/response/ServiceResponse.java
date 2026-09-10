package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
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
public class ServiceResponse {
    Long id;
    String name;
    BigDecimal unitPrice;
    String description;
    Date onCreate;
    Date onUpdate;
}
