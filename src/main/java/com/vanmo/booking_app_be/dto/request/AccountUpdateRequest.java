package com.vanmo.booking_app_be.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountUpdateRequest {

    String password;
    String role; // enum name
    Boolean status;
}
