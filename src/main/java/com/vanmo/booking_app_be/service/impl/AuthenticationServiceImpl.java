package com.vanmo.booking_app_be.service.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.vanmo.booking_app_be.dto.request.AuthenticationRequest;
import com.vanmo.booking_app_be.dto.request.IntrospectRequest;
import com.vanmo.booking_app_be.dto.request.LogoutRequest;
import com.vanmo.booking_app_be.dto.request.RefreshTokenRequest;
import com.vanmo.booking_app_be.dto.response.AuthenticationResponse;
import com.vanmo.booking_app_be.dto.response.IntrospectResponse;
import com.vanmo.booking_app_be.entity.*;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.mapper.CustomerMapper;
import com.vanmo.booking_app_be.mapper.EmployeeMapper;
import com.vanmo.booking_app_be.mapper.HotelMapper;
import com.vanmo.booking_app_be.repository.*;
import com.vanmo.booking_app_be.service.AccountService;
import com.vanmo.booking_app_be.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationServiceImpl implements AuthenticationService {
    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    static Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    AccountRepository accountRepository;
    InvalidatedRepository invalidatedRepository;
    HotelRepository hotelRepository;
    CustomerRepository customerRepository;
    EmployeeRepository employeeRepository;
    HotelMapper hotelMapper;
    CustomerMapper customerMapper;
    EmployeeMapper employeeMapper;

    @NonFinal
    @Value("${jwt.signer-key}")
    protected String SIGNER_KEY;
    @Override
    public IntrospectResponse introspect(IntrospectRequest request) throws ParseException, JOSEException {
        var token = request.getToken();

        boolean isValid = true;
        try {
            verifyToken(token);
        } catch (AppException e) {
            isValid = false;
        }

        return IntrospectResponse.builder().valid(isValid).build();
    }

    @Override
    public AuthenticationResponse authenticated(AuthenticationRequest request) {
        Account account = accountRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.INCORRECT_ACCOUNT_OR_PASSWORD));
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        boolean authenticated = passwordEncoder.matches(request.getPassword(), account.getPassword());

        if (!authenticated) throw new AppException(ErrorCode.INCORRECT_ACCOUNT_OR_PASSWORD);

        String jit = UUID.randomUUID().toString();
        var accessToken  = generateToken(account, "ACCESS", jit);
        var refreshToken  = generateToken(account, "REFRESH", jit);

        AuthenticationResponse authenticationResponse = AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authenticated(true)
                .role(account.getRole())
                .build();

        switch (account.getRole()){
            case Role.CUSTOMER:
                Customer customer = customerRepository.findByAccount_Id(account.getId());
                authenticationResponse.setCustomer(customerMapper.toResponse(customer));
                break;
            case Role.HOTEL_MANAGER:
                Hotel hotel = hotelRepository.findByAccount_Id(account.getId());
                authenticationResponse.setHotel(hotelMapper.toResponse(hotel));
                break;
            case Role.STAFF:
                Employee employee = employeeRepository.findByAccount_Id(account.getId());
                authenticationResponse.setEmployee(employeeMapper.toResponse(employee));
                break;
            default:
                throw new RuntimeException("Role not supported: " + account.getRole());
        }

        return authenticationResponse;
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshTokenRequest request)throws ParseException, JOSEException {
        SignedJWT signJWT = verifyToken(request.getToken());

        var claims = signJWT.getJWTClaimsSet();

        if (!"REFRESH".equals(claims.getStringClaim("token_type"))) {
            throw new AppException(ErrorCode.UN_AUTHENTICATED);
        }

        var jit = claims.getJWTID();
        var expiryTime = claims.getExpirationTime();

        invalidatedRepository.save(
                InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build());

        String username = claims.getSubject();

        Account account =
                accountRepository.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.UN_AUTHENTICATED));

        String jitNew = UUID.randomUUID().toString();
        var accessToken  = generateToken(account, "ACCESS", jitNew);
        var refreshToken  = generateToken(account, "REFRESH", jitNew);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    @Override
    public void logout()throws ParseException, JOSEException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new RuntimeException("Unauthenticated");
        }

        Jwt jwt = jwtAuth.getToken();
        String tokenValue = jwt.getTokenValue();

        SignedJWT  signedJWT  = verifyToken(tokenValue);

        String jti = signedJWT .getJWTClaimsSet().getJWTID();
        Date expiryTime = signedJWT .getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken =
                InvalidatedToken.builder().id(jti).expiryTime(expiryTime).build();

        invalidatedRepository.save(invalidatedToken);
    }

    private SignedJWT verifyToken(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        var verified = signedJWT.verify(verifier);
        if (!verified) throw new AppException(ErrorCode.UN_AUTHENTICATED);

        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();


        if (expiryTime.before(new Date())) throw new AppException(ErrorCode.UN_AUTHENTICATED);

        if (invalidatedRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UN_AUTHENTICATED);
        return signedJWT;
    }

    private String generateToken(Account account, String tokenType, String jit) {
        long duration = tokenType.equals("ACCESS")
                ? VALID_DURATION
                : REFRESHABLE_DURATION;
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(account.getUsername())
                .issuer("vanmo.com") // domain
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(duration, ChronoUnit.SECONDS).toEpochMilli())) // thời gian hết hạn
                .jwtID(jit)
                .claim("scope", buildScope(account))
                .claim("token_type", tokenType)
                .build();


        try {
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

            Payload payload = new Payload(jwtClaimsSet.toJSONObject());

            JWSObject jwsObject = new JWSObject(header, payload);

            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token ", e);
            throw new RuntimeException(e);
        }
    }

    private String buildScope(Account account) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (account.getRole() != null) stringJoiner.add("ROLE_" + account.getRole());

        return stringJoiner.toString();
    }
}
