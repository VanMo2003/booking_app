package com.vanmo.booking_app_be.service.impl;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.vanmo.booking_app_be.dto.request.DishCreateRequest;
import com.vanmo.booking_app_be.dto.request.DishUpdateRequest;
import com.vanmo.booking_app_be.dto.response.DishResponse;
import com.vanmo.booking_app_be.entity.Dish;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.DishMapper;
import com.vanmo.booking_app_be.repository.DishRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.DishService;
import com.vanmo.booking_app_be.service.FileStorageService;

import lombok.RequiredArgsConstructor;

/**
 * Food menu of a branch. Anyone may read it (public browsing); only the branch's HOTEL_OWNER or HOTEL_MANAGER
 * writes it - staff and admins are read-only ({@link AccessGuard#requireBranchManagerEditor(Long)}).
 */
@Service
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    private static final Comparator<Dish> MENU_ORDER = Comparator.comparing(
                    (Dish dish) -> dish.getCategory().ordinal())
            .thenComparing(Dish::getName, String.CASE_INSENSITIVE_ORDER);

    private final DishRepository dishRepository;
    private final DishMapper dishMapper;
    private final FileStorageService fileStorageService;
    private final AccessGuard accessGuard;

    @Override
    public DishResponse create(DishCreateRequest req) {
        Hotel hotel = accessGuard.requireBranchManagerEditor(req.getHotelId());
        Dish dish = dishMapper.toEntity(req);
        dish.setName(req.getName().trim());
        dish.setHotel(hotel);
        if (dish.getPathImage() != null && dish.getPathImage().isBlank()) {
            dish.setPathImage(null);
        }
        return dishMapper.toResponse(dishRepository.save(dish));
    }

    @Override
    public DishResponse update(Long id, DishUpdateRequest req) {
        Dish dish = findDish(id);
        accessGuard.requireBranchManagerEditor(dish.getHotel().getId());
        if (req.getName() != null && req.getName().isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "name: must not be blank");
        }
        dishMapper.updateEntity(dish, req);
        if (req.getName() != null) {
            dish.setName(req.getName().trim());
        }
        if (req.getPathImage() != null && req.getPathImage().isBlank()) {
            dish.setPathImage(null);
        }
        return dishMapper.toResponse(dishRepository.save(dish));
    }

    @Override
    public DishResponse uploadImage(Long id, MultipartFile file) {
        Dish dish = findDish(id);
        accessGuard.requireBranchManagerEditor(dish.getHotel().getId());
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "file: must not be empty");
        }
        dish.setPathImage(fileStorageService.uploadHotelImage(file));
        return dishMapper.toResponse(dishRepository.save(dish));
    }

    @Override
    public DishResponse getById(Long id) {
        return dishMapper.toResponse(findDish(id));
    }

    @Override
    public List<DishResponse> getAllByHotel(Long hotelId) {
        return dishRepository.findAllByHotel_Id(hotelId).stream()
                .sorted(MENU_ORDER)
                .map(dishMapper::toResponse)
                .toList();
    }

    @Override
    public void delete(Long id) {
        Dish dish = findDish(id);
        accessGuard.requireBranchManagerEditor(dish.getHotel().getId());
        dishRepository.delete(dish);
    }

    private Dish findDish(Long id) {
        return dishRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dish not found id=" + id));
    }
}
