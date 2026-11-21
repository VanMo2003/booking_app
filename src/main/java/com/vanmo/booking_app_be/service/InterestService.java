package com.vanmo.booking_app_be.service;

import java.util.Set;

import com.vanmo.booking_app_be.entity.enums.TravelTheme;

/** The signed-in guest's travel interests. */
public interface InterestService {
    Set<TravelTheme> mine();

    Set<TravelTheme> replace(Set<TravelTheme> interests);
}
