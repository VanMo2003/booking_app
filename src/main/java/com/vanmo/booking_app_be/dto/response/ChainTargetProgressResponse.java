package com.vanmo.booking_app_be.dto.response;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** The chain's month against the sum of its branches' goals, and each branch on its own. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChainTargetProgressResponse {
    TargetProgressResponse chain;
    List<TargetProgressResponse> branches;
}
