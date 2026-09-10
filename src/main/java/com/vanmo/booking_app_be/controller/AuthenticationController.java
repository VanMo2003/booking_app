package com.vanmo.booking_app_be.controller;

import java.text.ParseException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.JOSEException;
import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.AuthenticationRequest;
import com.vanmo.booking_app_be.dto.request.IntrospectRequest;
import com.vanmo.booking_app_be.dto.request.RefreshTokenRequest;
import com.vanmo.booking_app_be.dto.response.AuthenticationResponse;
import com.vanmo.booking_app_be.dto.response.IntrospectResponse;
import com.vanmo.booking_app_be.service.AuthenticationService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/login")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        return ApiResponse.ok(authenticationService.authenticated(request));
    }

    @PostMapping("/introspect")
    ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);
        return ApiResponse.ok(result);
    }

    @PostMapping("/logout")
    ApiResponse<Boolean> logout() throws ParseException, JOSEException {
        authenticationService.logout();
        return ApiResponse.ok(true);
    }

    @PostMapping("/refreshToken")
    ApiResponse<AuthenticationResponse> refreshToken(@RequestBody RefreshTokenRequest request)
            throws ParseException, JOSEException {
        var result = authenticationService.refreshToken(request);
        return ApiResponse.ok(result);
    }
}
