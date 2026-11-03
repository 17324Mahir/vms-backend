package com.starkgroup.vms.service;

import com.starkgroup.vms.dto.LoginRequest;
import com.starkgroup.vms.dto.LoginResponse;
import com.starkgroup.vms.dto.UserSummaryDto;
import com.starkgroup.vms.exception.InvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Implementation of AuthService managing course user authentication and JWT token issuance.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi";

    private final String courseUserEmail;
    private final String courseUserPasswordHash;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(
            @Value("${app.security.course-user.email:}") String courseUserEmail,
            @Value("${app.security.course-user.password-hash:}") String courseUserPasswordHash,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.courseUserEmail = courseUserEmail != null ? courseUserEmail.trim() : "";
        this.courseUserPasswordHash = courseUserPasswordHash != null ? courseUserPasswordHash.trim() : "";
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        final String inputEmail = request.email() != null
                ? request.email().trim().toLowerCase(Locale.ROOT)
                : "";

        if (courseUserEmail.isEmpty() || courseUserPasswordHash.isEmpty()) {
            logger.error("Authentication attempted but COURSE_USER_EMAIL or COURSE_USER_PASSWORD_HASH is not configured.");
            passwordEncoder.matches(request.password(), DUMMY_BCRYPT_HASH);
            throw new InvalidCredentialsException("Email or password is incorrect.");
        }

        final String configuredEmail = courseUserEmail.toLowerCase(Locale.ROOT);

        if (!configuredEmail.equals(inputEmail)) {
            passwordEncoder.matches(request.password(), DUMMY_BCRYPT_HASH);
            throw new InvalidCredentialsException("Email or password is incorrect.");
        }

        if (!passwordEncoder.matches(request.password(), courseUserPasswordHash)) {
            throw new InvalidCredentialsException("Email or password is incorrect.");
        }

        final String accessToken = jwtService.generateToken(courseUserEmail);
        final UserSummaryDto userSummary = new UserSummaryDto(courseUserEmail);

        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getExpirationSeconds(),
                userSummary
        );
    }
}
