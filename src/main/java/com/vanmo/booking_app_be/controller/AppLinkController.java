package com.vanmo.booking_app_be.controller;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vanmo.booking_app_be.entity.Tour;
import com.vanmo.booking_app_be.repository.TourRepository;
import com.vanmo.booking_app_be.service.impl.TourPricing;
import com.vanmo.booking_app_be.utils.AppLinks;

import lombok.RequiredArgsConstructor;

/**
 * Landing page for the links in emails. Mail clients drop custom-scheme links, so emails point here over plain
 * http(s) and this page hands the phone over to the app's {@code bookingapp://} deep link.
 */
@RestController
@RequestMapping("/app-links")
@RequiredArgsConstructor
public class AppLinkController {

    private final TourRepository tourRepository;

    private static final String TOUR_PAGE =
            """
			<!doctype html>
			<html lang="vi">
			<head>
			<meta charset="utf-8">
			<meta name="viewport" content="width=device-width, initial-scale=1">
			<title>%1$s</title>
			<meta property="og:title" content="%1$s">
			<meta property="og:description" content="%2$s">
			<meta property="og:image" content="%3$s">
			<meta property="og:type" content="website">
			<style>
			body { margin: 0; background: #EEF3F2; color: #132428;
					font-family: system-ui, -apple-system, "Segoe UI", Roboto, Arial, sans-serif; }
			main { max-width: 420px; margin: 0 auto; background: #FFFFFF; min-height: 100vh; }
			img { width: 100%%; height: 240px; object-fit: cover; display: block; background: #D6E3E1; }
			section { padding: 20px 20px 28px; }
			.place { font-size: 13px; color: #4D6166; margin: 0 0 6px; }
			h1 { margin: 0 0 10px; font-size: 21px; line-height: 28px; }
			.price { font-size: 20px; font-weight: 700; color: #0B6A61; margin: 0 0 4px; }
			.was { font-size: 13px; color: #7A8B8F; text-decoration: line-through; margin: 0 0 14px; }
			p.meta { font-size: 14px; line-height: 21px; color: #4D6166; margin: 0 0 22px; }
			a { display: block; padding: 14px; border-radius: 12px; background: #0B6A61; color: #FFFFFF;
				font-weight: 600; text-decoration: none; text-align: center; }
			a:focus-visible { outline: 3px solid #4A968D; outline-offset: 2px; }
			small { display: block; margin-top: 14px; font-size: 12px; line-height: 18px; color: #7A8B8F; text-align: center; }
			</style>
			</head>
			<body>
			<main>
			<img src="%3$s" alt="">
			<section>
			<p class="place">%4$s</p>
			<h1>%1$s</h1>
			<p class="price">%5$s / người lớn</p>
			%6$s
			<p class="meta">%2$s</p>
			<a id="open" href="%7$s">Xem và đặt tour trong Booking App</a>
			<small>Mở liên kết này trên điện thoại đã cài Booking App.</small>
			</section>
			</main>
			<script>
			var button = document.getElementById("open");
			if (/android/i.test(navigator.userAgent)) { button.href = "%8$s"; }
			</script>
			</body>
			</html>
			""";

    private static final Set<String> TARGETS = Set.of(AppLinks.OWNER_STATUS, AppLinks.NOTIFICATIONS);

    private static final String PAGE =
            """
			<!doctype html>
			<html lang="vi">
			<head>
			<meta charset="utf-8">
			<meta name="viewport" content="width=device-width, initial-scale=1">
			<title>Mở Booking App</title>
			<style>
			body { margin: 0; min-height: 100vh; display: flex; align-items: center; justify-content: center;
					background: #EEF3F2; color: #132428;
					font-family: system-ui, -apple-system, "Segoe UI", Roboto, Arial, sans-serif; }
			main { background: #FFFFFF; border-radius: 20px; padding: 32px 28px; margin: 24px; max-width: 360px;
					text-align: center; box-shadow: 0 8px 24px rgba(19, 36, 40, .08); }
			.mark { width: 64px; height: 64px; margin: 0 auto 18px; border-radius: 19px; background: #0B6A61;
					color: #FFFFFF; font-size: 28px; font-weight: 700; line-height: 64px; }
			h1 { margin: 0 0 8px; font-size: 20px; }
			p { margin: 0 0 22px; font-size: 14px; line-height: 21px; color: #4D6166; }
			a { display: block; padding: 14px; border-radius: 12px; background: #0B6A61; color: #FFFFFF;
				font-weight: 600; text-decoration: none; }
			a:focus-visible { outline: 3px solid #4A968D; outline-offset: 2px; }
			small { display: block; margin-top: 16px; font-size: 12px; line-height: 18px; color: #7A8B8F; }
			</style>
			</head>
			<body>
			<main>
			<div class="mark">B</div>
			<h1>Đang mở Booking App…</h1>
			<p>Nếu ứng dụng không tự mở, hãy nhấn nút bên dưới. Tài khoản của bạn vẫn được lưu đăng nhập trong ứng dụng.</p>
			<a id="open" href="%s">Mở ứng dụng</a>
			<small>Mở liên kết này trên điện thoại đã cài Booking App.</small>
			</main>
			<script>
			var button = document.getElementById("open");
			var link = "%s";
			if (/android/i.test(navigator.userAgent)) { link = "%s"; button.href = link; }
			setTimeout(function () { window.location.href = link; }, 300);
			</script>
			</body>
			</html>
			""";

    /** A shared tour: a small public page (also read by chat apps for the link preview) that opens the app. */
    @GetMapping("/tours/{id}")
    public ResponseEntity<String> tour(@PathVariable Long id) {
        Tour tour = tourRepository.findById(id).orElse(null);
        if (tour == null) {
            return ResponseEntity.notFound().build();
        }
        TourPricing.Price price = TourPricing.of(tour, TourPricing.today());
        String target = "tours/" + id;
        String deepLink = AppLinks.deepLink(target);
        String androidIntent = "intent://" + target + "#Intent;scheme=" + AppLinks.SCHEME + ";end";
        String meta = String.join(
                " · ",
                java.util.stream.Stream.of(
                                tour.getDuration(),
                                tour.getDeparture(),
                                tour.getHotel().getName())
                        .filter(text -> text != null && !text.isBlank())
                        .toList());
        String was = price.onSale() ? "<p class=\"was\">" + money(tour.getPrice()) + "</p>" : "";
        String image = tour.getPathImage() == null ? "" : tour.getPathImage();
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .body(TOUR_PAGE.formatted(
                        html(tour.getName()),
                        html(meta),
                        html(image),
                        html(tour.getDestination() == null ? "" : tour.getDestination()),
                        money(price.adult()),
                        was,
                        deepLink,
                        androidIntent));
    }

    private static String money(java.math.BigDecimal amount) {
        return String.format(java.util.Locale.ROOT, "%,d", amount.longValue()).replace(',', '.') + " ₫";
    }

    private static String html(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    @GetMapping("/{target}")
    public ResponseEntity<String> open(@PathVariable String target) {
        if (!TARGETS.contains(target)) {
            return ResponseEntity.notFound().build();
        }
        String deepLink = AppLinks.deepLink(target);
        // Chrome on Android only follows custom schemes reliably through an intent:// URL.
        String androidIntent = "intent://" + target + "#Intent;scheme=" + AppLinks.SCHEME + ";end";
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .body(PAGE.formatted(deepLink, deepLink, androidIntent));
    }
}
