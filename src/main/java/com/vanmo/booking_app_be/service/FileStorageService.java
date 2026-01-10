package com.vanmo.booking_app_be.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String uploadHotelImage(MultipartFile file);
}
