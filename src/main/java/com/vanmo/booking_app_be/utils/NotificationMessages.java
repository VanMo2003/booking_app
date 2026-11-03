package com.vanmo.booking_app_be.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.vanmo.booking_app_be.entity.HotelChain;
import com.vanmo.booking_app_be.entity.TourBooking;

/** Vietnamese copy of the notifications shown in the app and pushed to phones, kept in one place. */
public final class NotificationMessages {

    public record Message(String title, String body) {}

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter SHORT_DAY = DateTimeFormatter.ofPattern("dd/MM");

    private NotificationMessages() {}

    /** The "Gửi thông báo thử" button: proves pushes reach this phone even with the app closed. */
    public static Message testPush() {
        return new Message(
                "Thông báo thử",
                "Máy đã nhận được thông báo khi app chạy nền hoặc đã tắt — bạn sẽ không bỏ lỡ tin nhắn và đơn mới.");
    }

    public static Message ownerSubmitted(HotelChain chain, boolean resubmitted) {
        if (resubmitted) {
            return new Message(
                    "Hồ sơ chủ khách sạn được gửi lại",
                    chain.getName() + " đã cập nhật hồ sơ và đang chờ xét duyệt lại.");
        }
        String owner = isBlank(chain.getOwnerName()) ? "" : " (" + chain.getOwnerName() + ")";
        return new Message("Hồ sơ chủ khách sạn mới", chain.getName() + owner + " vừa đăng ký và đang chờ xét duyệt.");
    }

    public static Message ownerApproved(HotelChain chain) {
        return new Message(
                "Tài khoản chủ khách sạn đã được duyệt",
                "Khách sạn " + chain.getName()
                        + " đã sẵn sàng. Mở ứng dụng để thêm quản lý, phòng, tiện ích và dịch vụ.");
    }

    public static Message ownerRejected(HotelChain chain, String reason) {
        String trimmed = reason.trim();
        String separator = trimmed.matches(".*[.!?…]$") ? " " : ". ";
        return new Message(
                "Hồ sơ " + chain.getName() + " chưa được duyệt",
                "Lý do: " + trimmed + separator + "Cập nhật thông tin rồi gửi lại trong ứng dụng.");
    }

    /** Push for the guest: the branch's name as title, like a messenger app shows the contact. */
    public static Message chatToCustomer(String hotelName, String content) {
        return new Message(hotelName, preview(content));
    }

    /** Push for the branch team; the branch name is there because an owner answers for several branches. */
    public static Message chatToHotel(String customerName, String hotelName, String content) {
        String sender = isBlank(customerName) ? "Khách hàng" : customerName;
        return new Message(sender + " · " + hotelName, preview(content));
    }

    /** For the branch team: who ordered what, so the push alone is enough to act on. */
    public static Message tourBookingCreated(TourBooking booking) {
        String guest = isBlank(booking.getCustomer().getFullName())
                ? "Khách hàng"
                : booking.getCustomer().getFullName();
        return new Message(
                "Đơn tour mới #" + booking.getId() + " · " + booking.getHotel().getName(),
                guest + " đặt " + tourSummary(booking) + (booking.isCreatedByAi() ? " qua trợ lý AI" : "")
                        + ". Chờ khách sạn xác nhận.");
    }

    public static Message tourBookingConfirmed(TourBooking booking) {
        return new Message(
                "Khách sạn đã xác nhận tour",
                booking.getHotel().getName() + " xác nhận " + tourSummary(booking) + ". Thanh toán tại khách sạn.");
    }

    public static Message tourBookingCanceledByHotel(TourBooking booking) {
        return new Message(
                "Đơn tour #" + booking.getId() + " đã bị huỷ",
                booking.getHotel().getName() + " huỷ " + tourSummary(booking) + reasonSuffix(booking));
    }

    public static Message tourBookingCanceledByCustomer(TourBooking booking) {
        String guest = isBlank(booking.getCustomer().getFullName())
                ? "Khách hàng"
                : booking.getCustomer().getFullName();
        return new Message(
                "Khách huỷ đơn tour #" + booking.getId() + " · "
                        + booking.getHotel().getName(),
                guest + " huỷ " + tourSummary(booking) + reasonSuffix(booking));
    }

    /** "Vịnh Hạ Long 1 ngày · 18/10/2026 · 2 khách (2.580.000 ₫)" */
    private static String tourSummary(TourBooking booking) {
        return booking.getTourName() + " · " + booking.getTourDate().format(DAY) + " · " + booking.getGuests()
                + " khách" + rooms(booking) + " (" + money(booking.getTotalAmount()) + ")";
    }

    /** " · phòng 101, 102" for a tour sold with rooms. */
    private static String rooms(TourBooking booking) {
        if (booking.getBooking() == null || booking.getBooking().getBookingRooms() == null) {
            return "";
        }
        List<String> numbers = booking.getBooking().getBookingRooms().stream()
                .map(link -> link.getRoom().getRoomNumber())
                .sorted()
                .toList();
        return numbers.isEmpty() ? "" : " · phòng " + String.join(", ", numbers);
    }

    private static String reasonSuffix(TourBooking booking) {
        return isBlank(booking.getCancelReason())
                ? "."
                : ". Lý do: " + booking.getCancelReason().strip();
    }

    public static Message revenueTargetReached(
            String hotelName, YearMonth month, BigDecimal revenue, BigDecimal target) {
        return new Message(
                "Đạt mục tiêu tháng " + month.format(MONTH) + " · " + hotelName,
                "Doanh thu đã đạt " + money(revenue) + ", vượt mục tiêu " + money(target) + ". Chúc mừng cả đội!");
    }

    public static Message revenueTargetAtRisk(
            String hotelName, YearMonth month, BigDecimal projected, BigDecimal target, double projectedRate) {
        return new Message(
                "Nguy cơ hụt mục tiêu tháng " + month.format(MONTH) + " · " + hotelName,
                "Dự kiến cả tháng chỉ " + money(projected) + " (" + Math.round(projectedRate) + "% mục tiêu "
                        + money(target) + "). Xem báo cáo để có kế hoạch kích cầu.");
    }

    /** Last week's figures for a chain or a branch, against the week before. */
    public static Message weeklyReport(
            String scopeName,
            LocalDate from,
            LocalDate to,
            BigDecimal revenue,
            Double revenueGrowth,
            double occupancyRate,
            long completedBookings,
            long canceledBookings,
            Double bookedAhead) {
        String change = revenueGrowth == null
                ? ""
                : " (" + (revenueGrowth >= 0 ? "+" : "") + Math.round(revenueGrowth) + "% so với tuần trước)";
        String ahead = bookedAhead == null ? "" : " Tuần này đã đặt " + Math.round(bookedAhead) + "% phòng.";
        return new Message(
                "Báo cáo tuần " + from.format(SHORT_DAY) + " – " + to.format(SHORT_DAY) + " · " + scopeName,
                "Doanh thu " + money(revenue) + change + " · lấp đầy " + Math.round(occupancyRate) + "% · "
                        + completedBookings + " đơn hoàn tất · " + canceledBookings + " đơn huỷ." + ahead);
    }

    private static String money(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(amount) + " ₫";
    }

    public static String preview(String content) {
        String flat = content.strip().replaceAll("\\s+", " ");
        return flat.length() <= 120 ? flat : flat.substring(0, 119) + "…";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
