package com.vanmo.booking_app_be.mapper;

import org.mapstruct.Mapper;

import com.vanmo.booking_app_be.dto.response.NotificationResponse;
import com.vanmo.booking_app_be.entity.Notification;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponse toResponse(Notification notification);
}
