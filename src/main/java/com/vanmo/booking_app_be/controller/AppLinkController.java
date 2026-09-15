package com.vanmo.booking_app_be.controller;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vanmo.booking_app_be.utils.AppLinks;

/**
 * Landing page for the links in emails. Mail clients drop custom-scheme links, so emails point here over plain
 * http(s) and this page hands the phone over to the app's {@code bookingapp://} deep link.
 */
@RestController
@RequestMapping("/app-links")
public class AppLinkController {

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
