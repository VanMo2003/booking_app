package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;

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
public class StaffPerformanceDTO {
    String accountId;
    String username;
    /** From the Employee record when there is one (managers don't have one). */
    String fullName;

    String positionName;
    /** Bookings the account took at the desk checking in during the period, not cancelled. */
    Long totalBooking;

    /** What those bookings are worth (some may not be completed yet). */
    BigDecimal totalRevenue;

    /** Bookings the account took that completed with check-out in the period... */
    long completedBookings;

    /** ...and what they earned. */
    BigDecimal completedRevenue;

    /** Completed tour orders (tour date in the period) the account confirmed or completed last... */
    long toursHandled;

    /** ...and their tour revenue. */
    BigDecimal tourRevenue;

    /** JPQL projection of the bookings an account took; the completed and tour figures are added afterwards. */
    public StaffPerformanceDTO(
            String accountId,
            String username,
            String fullName,
            String positionName,
            Long totalBooking,
            BigDecimal totalRevenue) {
        this(
                accountId,
                username,
                fullName,
                positionName,
                totalBooking,
                totalRevenue,
                0L,
                BigDecimal.ZERO,
                0L,
                BigDecimal.ZERO);
    }
}
