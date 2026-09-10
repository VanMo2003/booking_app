package com.vanmo.booking_app_be.service;

import java.text.ParseException;

import com.nimbusds.jose.JOSEException;
import com.vanmo.booking_app_be.dto.request.AuthenticationRequest;
import com.vanmo.booking_app_be.dto.request.IntrospectRequest;
import com.vanmo.booking_app_be.dto.request.RefreshTokenRequest;
import com.vanmo.booking_app_be.dto.response.AuthenticationResponse;
import com.vanmo.booking_app_be.dto.response.IntrospectResponse;

public interface AuthenticationService {
    IntrospectResponse introspect(IntrospectRequest request) throws ParseException, JOSEException;

    AuthenticationResponse authenticated(AuthenticationRequest request);

    void logout() throws ParseException, JOSEException;

    AuthenticationResponse refreshToken(RefreshTokenRequest request) throws ParseException, JOSEException;
}
