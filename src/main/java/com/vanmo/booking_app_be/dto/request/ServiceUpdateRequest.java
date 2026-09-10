package com.vanmo.booking_app_be.dto.request;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceUpdateRequest {
    String name;
    BigDecimal unitPrice;
    String description;
}
