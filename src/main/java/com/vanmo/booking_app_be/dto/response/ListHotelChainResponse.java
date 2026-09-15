package com.vanmo.booking_app_be.dto.response;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vanmo.booking_app_be.entity.enums.ApprovalStatus;

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
    String ownerUsername;
    String ownerName;
    String email;
    String phone;
    String address;
    ApprovalStatus approvalStatus;
    String rejectionReason;
    Date submittedAt;
    Date reviewedAt;
    Date onCreate;
    Date onUpdate;
}
