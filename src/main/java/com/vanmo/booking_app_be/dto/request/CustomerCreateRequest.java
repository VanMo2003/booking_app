package com.vanmo.booking_app_be.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerCreateRequest {
    String pathImage;
    @NotNull String accountId;
    String fullName;
    @Pattern(regexp = "0\\d{9}")
    @Length(min = 10, max = 10)
    @NotNull
    String phoneNumber;
    String gender;
    String hometown;
}
