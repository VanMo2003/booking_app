package com.vanmo.booking_app_be.ai;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vanmo.booking_app_be.entity.Amenity;
import com.vanmo.booking_app_be.entity.Dish;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Room;
import com.vanmo.booking_app_be.entity.RoomAmenity;
import com.vanmo.booking_app_be.entity.Service;
import com.vanmo.booking_app_be.entity.Tour;
import com.vanmo.booking_app_be.entity.enums.DishCategory;
import com.vanmo.booking_app_be.entity.enums.RoomStatus;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.repository.AmenityRepository;
import com.vanmo.booking_app_be.repository.BookingRoomRepository;
import com.vanmo.booking_app_be.repository.DishRepository;
import com.vanmo.booking_app_be.repository.HotelRepository;
import com.vanmo.booking_app_be.repository.RoomAmenityRepository;
import com.vanmo.booking_app_be.repository.RoomRepository;
import com.vanmo.booking_app_be.repository.ServiceRepository;
import com.vanmo.booking_app_be.repository.TourRepository;

import lombok.RequiredArgsConstructor;

/**
 * What the AI is allowed to know about a branch, read straight from the database: a fact sheet for the prompt and a
 * real availability lookup for tool calls. Everything here is public browse data - no bookings, customers or staff -
 * so it is safe to put in front of a model and needs no caller.
 */
@Component
@RequiredArgsConstructor
public class HotelKnowledge {

    /** Longest stay the tools look up - keeps a single call bounded. */
    public static final int MAX_NIGHTS = 30;

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final AmenityRepository amenityRepository;
    private final RoomAmenityRepository roomAmenityRepository;
    private final ServiceRepository serviceRepository;
    private final DishRepository dishRepository;
    private final TourRepository tourRepository;
    private final BookingRoomRepository bookingRoomRepository;

    /** A room free for a stay (or, without dates, just a room that isn't under maintenance). */
    public record RoomOption(
            Long roomId,
            String roomNumber,
            String roomType,
            int capacity,
            BigDecimal pricePerNight,
            List<String> amenities) {}

    @Transactional(readOnly = true)
    public Hotel hotel(Long hotelId) {
        return hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id=" + hotelId));
    }

    /** Plain-text profile of the branch for the assistant's system prompt. */
    @Transactional(readOnly = true)
    public String factSheet(Long hotelId) {
        Hotel hotel = hotel(hotelId);
        List<Room> rooms = roomRepository.findAllByHotel_Id(hotelId);
        StringBuilder out = new StringBuilder("HỒ SƠ CƠ SỞ (dữ liệu hệ thống)\n");
        out.append("Tên: ").append(hotel.getName()).append('\n');
        out.append("Loại hình: ").append(hotel.getCategory());
        if (hotel.getRating() > 0) {
            out.append(" · Đánh giá ").append(Math.round(hotel.getRating())).append("/5");
        }
        out.append('\n');
        out.append("Địa chỉ: ").append(hotel.getAddress()).append('\n');
        out.append("Điện thoại: ").append(hotel.getPhone()).append('\n');
        out.append("Tình trạng: ")
                .append(hotel.isActive() ? "đang nhận đặt phòng" : "tạm ngừng nhận đặt phòng")
                .append('\n');
        if (hotel.getDescription() != null && !hotel.getDescription().isBlank()) {
            out.append("Giới thiệu: ").append(hotel.getDescription().strip()).append('\n');
        }

        List<String> common = amenityRepository.findAllByHotel_Id(hotelId).stream()
                .filter(Amenity::isCommon)
                .map(Amenity::getName)
                .toList();
        out.append("\nTiện ích chung: ")
                .append(common.isEmpty() ? "(chưa cập nhật)" : String.join("; ", common))
                .append('\n');

        out.append("\nLoại phòng (giá mỗi đêm):\n");
        Map<String, List<Room>> byType = rooms.stream()
                .collect(Collectors.groupingBy(r -> r.getRoomType().getName(), TreeMap::new, Collectors.toList()));
        if (byType.isEmpty()) {
            out.append("- (chưa có phòng)\n");
        }
        byType.forEach((type, group) -> {
            BigDecimal min = group.stream()
                    .map(Room::getPrice)
                    .min(Comparator.naturalOrder())
                    .orElseThrow();
            BigDecimal max = group.stream()
                    .map(Room::getPrice)
                    .max(Comparator.naturalOrder())
                    .orElseThrow();
            int capacity = group.stream().mapToInt(Room::getCapacity).max().orElse(0);
            Set<String> amenities = new HashSet<>();
            group.forEach(room -> amenities.addAll(roomAmenities(room.getId())));
            out.append("- ")
                    .append(type)
                    .append(": ")
                    .append(group.size())
                    .append(" phòng, tối đa ")
                    .append(capacity)
                    .append(" khách, ")
                    .append(min.compareTo(max) == 0 ? money(min) : money(min) + " – " + money(max));
            if (!amenities.isEmpty()) {
                out.append("; tiện ích trong phòng: ").append(String.join(", ", new TreeSet<>(amenities)));
            }
            out.append('\n');
        });

        List<Service> services = serviceRepository.findAllByHotel_Id(hotelId);
        out.append("\nDịch vụ tính thêm:\n");
        if (services.isEmpty()) {
            out.append("- (chưa có)\n");
        }
        for (Service service : services) {
            out.append("- ").append(service.getName()).append(": ").append(money(service.getUnitPrice()));
            if (service.getDescription() != null && !service.getDescription().isBlank()) {
                out.append(" — ").append(service.getDescription().strip());
            }
            out.append('\n');
        }

        List<Dish> dishes = dishRepository.findAllByHotel_Id(hotelId);
        out.append("\nThực đơn nhà hàng:\n");
        if (dishes.isEmpty()) {
            out.append("- (chưa cập nhật)\n");
        }
        Map<DishCategory, List<Dish>> menu =
                new TreeMap<>(dishes.stream().collect(Collectors.groupingBy(Dish::getCategory)));
        menu.forEach((category, items) -> out.append("- ")
                .append(categoryLabel(category))
                .append(": ")
                .append(items.stream()
                        .sorted(Comparator.comparing(Dish::getName))
                        .map(d -> d.getName() + " " + money(d.getPrice()) + (d.isAvailable() ? "" : " (tạm hết)"))
                        .collect(Collectors.joining("; ")))
                .append('\n'));

        out.append("\nTour tham quan (giá mỗi khách, đặt qua nhân viên khách sạn):\n");
        List<Tour> tours = tourRepository.findAllByHotel_Id(hotelId);
        if (tours.isEmpty()) {
            out.append("- (chưa có)\n");
        }
        tours.stream()
                .sorted(Comparator.comparing((Tour t) -> !t.isAvailable()).thenComparing(Tour::getPrice))
                .forEach(tour -> out.append(tourLine(tour)).append('\n'));

        out.append(
                "\nChưa có trong hệ thống: giờ nhận/trả phòng, chính sách huỷ và hoàn tiền, khuyến mãi, phí phụ thu.\n");
        return out.toString();
    }

    /** "- Food tour phố cổ: 450.000 ₫/khách; 4 giờ; khởi hành 18:00 hằng ngày; gồm …; tối đa 10 khách. Mô tả…" */
    private static String tourLine(Tour tour) {
        StringBuilder line = new StringBuilder("- ").append(tour.getName());
        if (!tour.isAvailable()) {
            line.append(" (TẠM NGỪNG, không nhận khách)");
        }
        line.append(": ").append(money(tour.getPrice())).append("/khách");
        if (tour.getDuration() != null) {
            line.append("; ").append(tour.getDuration());
        }
        if (tour.getDeparture() != null) {
            line.append("; khởi hành ").append(tour.getDeparture());
        }
        if (tour.getIncludes() != null) {
            line.append("; gồm ").append(tour.getIncludes());
        }
        if (tour.getMaxGuests() != null) {
            line.append("; tối đa ").append(tour.getMaxGuests()).append(" khách");
        }
        if (tour.getDescription() != null) {
            String description = tour.getDescription().replaceAll("\\s+", " ");
            line.append(". ").append(description.length() > 300 ? description.substring(0, 300) + "…" : description);
        }
        return line.toString();
    }

    /**
     * Rooms bookable for [checkin, checkout) - same rule as the hotel detail page: not under maintenance, not occupied,
     * not in a PENDING/CONFIRMED booking that overlaps - with enough beds and within the price range. Cheapest first.
     */
    @Transactional(readOnly = true)
    public List<RoomOption> availableRooms(
            Long hotelId,
            LocalDate checkin,
            LocalDate checkout,
            Integer guests,
            BigDecimal minPrice,
            BigDecimal maxPrice) {
        boolean dated = checkin != null && checkout != null;
        Set<Long> booked = dated ? new HashSet<>(bookingRoomRepository.findBookedRoomIds(checkin, checkout)) : Set.of();
        List<RoomOption> options = new ArrayList<>();
        for (Room room : roomRepository.findAllByHotel_Id(hotelId)) {
            if (room.getStatus() == RoomStatus.MAINTENANCE
                    || (dated && room.getStatus() == RoomStatus.OCCUPIED)
                    || booked.contains(room.getId())) {
                continue;
            }
            if (guests != null && room.getCapacity() < guests) {
                continue;
            }
            if (minPrice != null && room.getPrice().compareTo(minPrice) < 0) {
                continue;
            }
            if (maxPrice != null && room.getPrice().compareTo(maxPrice) > 0) {
                continue;
            }
            options.add(new RoomOption(
                    room.getId(),
                    room.getRoomNumber(),
                    room.getRoomType().getName(),
                    room.getCapacity(),
                    room.getPrice(),
                    roomAmenities(room.getId())));
        }
        options.sort(Comparator.comparing(RoomOption::pricePerNight).thenComparing(RoomOption::roomNumber));
        return options;
    }

    /** Every amenity name at the branch, common and per-room - used to match what a search asks for. */
    @Transactional(readOnly = true)
    public List<String> amenityNames(Long hotelId) {
        return amenityRepository.findAllByHotel_Id(hotelId).stream()
                .map(Amenity::getName)
                .distinct()
                .toList();
    }

    /** Checks a stay the model asked for; returns the problem in Vietnamese, or null when it is usable. */
    public static String validateStay(LocalDate checkin, LocalDate checkout, LocalDate today) {
        if (checkin == null || checkout == null) {
            return "Thiếu ngày nhận hoặc ngày trả phòng.";
        }
        if (!checkout.isAfter(checkin)) {
            return "Ngày trả phòng phải sau ngày nhận phòng.";
        }
        if (checkin.isBefore(today)) {
            return "Ngày nhận phòng đã qua.";
        }
        if (ChronoUnit.DAYS.between(checkin, checkout) > MAX_NIGHTS) {
            return "Chỉ tra cứu được tối đa " + MAX_NIGHTS + " đêm mỗi lần.";
        }
        return null;
    }

    public static String money(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(amount) + " ₫";
    }

    private List<String> roomAmenities(Long roomId) {
        return roomAmenityRepository.findAllByRoomId(roomId).stream()
                .map(RoomAmenity::getAmenity)
                .map(Amenity::getName)
                .distinct()
                .toList();
    }

    private static String categoryLabel(DishCategory category) {
        return switch (category) {
            case APPETIZER -> "Khai vị";
            case MAIN_COURSE -> "Món chính";
            case SIDE_DISH -> "Món phụ";
            case DESSERT -> "Tráng miệng";
            case DRINK -> "Đồ uống";
            case OTHER -> "Khác";
        };
    }
}
