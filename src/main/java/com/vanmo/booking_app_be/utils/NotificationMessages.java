package com.vanmo.booking_app_be.utils;

import com.vanmo.booking_app_be.entity.HotelChain;

/** Vietnamese copy of the notifications shown in the app and pushed to phones, kept in one place. */
public final class NotificationMessages {

    public record Message(String title, String body) {}

    private NotificationMessages() {}

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

    public static String preview(String content) {
        String flat = content.strip().replaceAll("\\s+", " ");
        return flat.length() <= 120 ? flat : flat.substring(0, 119) + "…";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
