package com.vanmo.booking_app_be.service.impl;

import java.io.UnsupportedEncodingException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.vanmo.booking_app_be.event.OwnerReviewedEvent;
import com.vanmo.booking_app_be.service.MailService;
import com.vanmo.booking_app_be.utils.AppLinks;

import lombok.extern.slf4j.Slf4j;

/** SMTP mail (MAIL_* variables). Without MAIL_USERNAME the mail is only logged, so local runs need no mail server. */
@Slf4j
@Service
public class MailServiceImpl implements MailService {

    private static final String TEMPLATE =
            """
			<!doctype html>
			<html lang="vi">
			<head>
			<meta charset="utf-8">
			<meta name="viewport" content="width=device-width, initial-scale=1">
			</head>
			<body style="margin:0;padding:0;background:#EEF3F2;font-family:Arial,Helvetica,sans-serif;color:#132428">
			<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#EEF3F2;padding:24px 12px">
				<tr><td align="center">
				<table role="presentation" width="560" cellpadding="0" cellspacing="0" style="width:100%%;max-width:560px;background:#FFFFFF;border-radius:16px;overflow:hidden">
					<tr><td style="background:#0B6A61;padding:24px 32px;color:#FFFFFF">
					<div style="font-size:12px;letter-spacing:1px;text-transform:uppercase;color:#CFE6E2">Booking App &middot; Đối tác khách sạn</div>
					<div style="font-size:22px;line-height:30px;font-weight:bold;margin-top:6px">%s</div>
					</td></tr>
					<tr><td style="padding:28px 32px 8px;font-size:15px;line-height:23px">
					<p style="margin:0 0 12px">Chào %s,</p>
					<p style="margin:0">%s</p>
					</td></tr>
					%s
					<tr><td style="padding:20px 32px 8px">
					<a href="%s" style="display:inline-block;background:#0B6A61;color:#FFFFFF;text-decoration:none;font-weight:bold;font-size:15px;padding:13px 24px;border-radius:10px">Mở ứng dụng Booking App</a>
					</td></tr>
					<tr><td style="padding:12px 32px 28px;font-size:13px;line-height:20px;color:#4D6166">
					Hãy mở email này trên điện thoại đã cài Booking App. Tài khoản của bạn vẫn được lưu đăng nhập trong ứng dụng nên không cần đăng nhập lại.<br>
					<span style="color:#7A8B8F">Nếu nút không hoạt động, mở liên kết: %s</span>
					</td></tr>
				</table>
				</td></tr>
			</table>
			</body>
			</html>
			""";

    private static final String REASON_TEMPLATE =
            """
			<tr><td style="padding:16px 32px 0">
			<div style="background:#FBEDEB;border-radius:10px;padding:14px 16px;font-size:14px;line-height:21px;color:#132428">
				<div style="font-weight:bold;color:#C03A2B;margin-bottom:4px">Lý do từ chối</div>
				%s
			</div>
			</td></tr>
			""";

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String username;
    private final String from;
    private final String publicUrl;

    public MailServiceImpl(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${spring.mail.username:}") String username,
            @Value("${app.mail.from:}") String from,
            @Value("${app.public-url}") String publicUrl) {
        this.mailSender = mailSender;
        this.username = username;
        this.from = from;
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }

    @Override
    public void sendOwnerReview(OwnerReviewedEvent review) {
        if (review.email() == null || review.email().isBlank()) {
            return;
        }
        String subject = review.approved()
                ? "[Booking App] Tài khoản chủ khách sạn đã được duyệt"
                : "[Booking App] Hồ sơ chủ khách sạn chưa được duyệt";
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || username.isBlank()) {
            log.info("Mail is not configured (MAIL_USERNAME) - skipped \"{}\" to {}", subject, review.email());
            return;
        }

        String link = publicUrl + "/app-links/" + AppLinks.OWNER_STATUS;
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new InternetAddress(from.isBlank() ? username : from, "Booking App", "UTF-8"));
            helper.setTo(review.email());
            helper.setSubject(subject);
            helper.setText(plainText(review, link), html(review, link));
            sender.send(message);
            log.info("Sent \"{}\" to {}", subject, review.email());
        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            log.error("Cannot send \"{}\" to {}", subject, review.email(), e);
        }
    }

    private static String html(OwnerReviewedEvent review, String link) {
        String hotel = escape(review.hotelName());
        String headline = review.approved() ? "Tài khoản đã được duyệt" : "Hồ sơ chưa được duyệt";
        String message = review.approved()
                ? "Hồ sơ khách sạn <strong>" + hotel + "</strong> đã được duyệt. Đăng nhập bằng tài khoản <strong>"
                        + escape(review.ownerUsername())
                        + "</strong> để thêm quản lý, phòng, tiện ích và dịch vụ cho khách sạn của bạn."
                : "Hồ sơ khách sạn <strong>" + hotel
                        + "</strong> chưa được duyệt. Bạn có thể cập nhật thông tin và gửi lại ngay trong ứng dụng.";
        String reason = review.approved() ? "" : REASON_TEMPLATE.formatted(escape(review.reason()));
        return TEMPLATE.formatted(headline, escape(displayName(review)), message, reason, link, link);
    }

    private static String plainText(OwnerReviewedEvent review, String link) {
        StringBuilder text =
                new StringBuilder("Chào ").append(displayName(review)).append(",\n\n");
        if (review.approved()) {
            text.append("Hồ sơ khách sạn ")
                    .append(review.hotelName())
                    .append(" đã được duyệt. Đăng nhập bằng tài khoản ")
                    .append(review.ownerUsername())
                    .append(" để thêm quản lý, phòng, tiện ích và dịch vụ.\n");
        } else {
            text.append("Hồ sơ khách sạn ")
                    .append(review.hotelName())
                    .append(" chưa được duyệt.\nLý do: ")
                    .append(review.reason())
                    .append("\nBạn có thể cập nhật thông tin và gửi lại trong ứng dụng.\n");
        }
        return text.append("\nMở ứng dụng: ").append(link).append('\n').toString();
    }

    private static String displayName(OwnerReviewedEvent review) {
        return review.ownerName() == null || review.ownerName().isBlank() ? review.ownerUsername() : review.ownerName();
    }

    private static String escape(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value, "UTF-8");
    }
}
