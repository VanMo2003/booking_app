package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vanmo.booking_app_be.dto.request.ItineraryStepRequest;
import com.vanmo.booking_app_be.entity.enums.TravelTheme;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TourResponse {
    Long id;
    String name;
    String description;
    BigDecimal price;
    String duration;
    String departure;
    String includes;
    Integer maxGuests;
    String pathImage;
    boolean available;
    Integer stayNights;
    /** Rooms the package uses, cheapest first; empty when the tour is sold without a room. */
    List<TourRoomResponse> rooms;

    Long hotelId;
    String hotelName;
    String hotelAddress;
    String destination;
    Set<TravelTheme> themes;
    List<ItineraryStepRequest> itinerary;
    /** Null = children pay the adult price. */
    BigDecimal childPrice;

    String cancellationPolicy;
    boolean featured;
    Integer discountPercent;
    LocalDate promotionEndsAt;
    /** The promotion is running today: salePrice / saleChildPrice are what a guest pays. */
    boolean onSale;

    BigDecimal salePrice;
    BigDecimal saleChildPrice;
    Date onCreate;
    Date onUpdate;
}
