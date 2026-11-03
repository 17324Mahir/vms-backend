package com.starkgroup.vms.service;

import com.starkgroup.vms.domain.User;
import com.starkgroup.vms.dto.LoginRequest;
import com.starkgroup.vms.dto.LoginResponse;
import com.starkgroup.vms.dto.RegisterRequest;
import com.starkgroup.vms.dto.UserSummaryDto;
import com.starkgroup.vms.exception.DuplicateEmailException;
import com.starkgroup.vms.exception.InvalidCredentialsException;
import com.starkgroup.vms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * Implementation of AuthService managing user registration, authentication, and JWT token issuance.
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
    private final UserRepository userRepository;

    public AuthServiceImpl(
            @Value("${app.security.course-user.email:}") String courseUserEmail,
            @Value("${app.security.course-user.password-hash:}") String courseUserPasswordHash,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this(courseUserEmail, courseUserPasswordHash, passwordEncoder, jwtService, null);
    }

    @Autowired
    public AuthServiceImpl(
            @Value("${app.security.course-user.email:}") String courseUserEmail,
            @Value("${app.security.course-user.password-hash:}") String courseUserPasswordHash,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Autowired(required = false) UserRepository userRepository) {
        this.courseUserEmail = courseUserEmail != null ? courseUserEmail.trim() : "";
        this.courseUserPasswordHash = courseUserPasswordHash != null ? courseUserPasswordHash.trim() : "";
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        final String rawEmail = request.email() != null ? request.email().trim().toLowerCase(Locale.ROOT) : "";
        final String rawName = request.name() != null ? request.name().trim() : "";
        final String rawPassword = request.password() != null ? request.password().trim() : "";

        if (rawEmail.isEmpty() || rawName.isEmpty() || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Name, email, and password are required.");
        }

        // Check if email conflicts with course user
        if (!courseUserEmail.isEmpty() && courseUserEmail.equalsIgnoreCase(rawEmail)) {
            throw new DuplicateEmailException("An account with this email address already exists.");
        }

        // Check if email conflicts with registered users in database
        if (userRepository != null && userRepository.existsByEmailIgnoreCase(rawEmail)) {
            throw new DuplicateEmailException("An account with this email address already exists.");
        }

        final String encodedPassword = passwordEncoder.encode(rawPassword);
        final User newUser = new User(rawName, rawEmail, encodedPassword, "USER");

        if (userRepository != null) {
            userRepository.save(newUser);
            logger.info("Successfully registered new user: {}", rawEmail);
        } else {
            logger.warn("UserRepository is unavailable; user record not saved to database.");
        }

        final String accessToken = jwtService.generateToken(rawEmail);
        final UserSummaryDto userSummary = new UserSummaryDto(rawEmail);

        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getExpirationSeconds(),
                userSummary
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        final String inputEmail = request.email() != null
                ? request.email().trim().toLowerCase(Locale.ROOT)
                : "";

        // 1. First check dynamic users database if repository is available
        if (userRepository != null) {
            Optional<User> userOpt = userRepository.findByEmailIgnoreCase(inputEmail);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                    final String accessToken = jwtService.generateToken(user.getEmail());
                    final UserSummaryDto userSummary = new UserSummaryDto(user.getEmail());
                    return new LoginResponse(
                            accessToken,
                            "Bearer",
                            jwtService.getExpirationSeconds(),
                            userSummary
                    );
                } else {
                    throw new InvalidCredentialsException("Email or password is incorrect.");
                }
            }
        }

        // 2. Fall back to designated course user
        if (!courseUserEmail.isEmpty() && !courseUserPasswordHash.isEmpty()) {
            final String configuredEmail = courseUserEmail.toLowerCase(Locale.ROOT);
            if (configuredEmail.equals(inputEmail)) {
                if (passwordEncoder.matches(request.password(), courseUserPasswordHash)) {
                    final String accessToken = jwtService.generateToken(courseUserEmail);
                    final UserSummaryDto userSummary = new UserSummaryDto(courseUserEmail);
                    return new LoginResponse(
                            accessToken,
                            "Bearer",
                            jwtService.getExpirationSeconds(),
                            userSummary
                    );
                } else {
                    throw new InvalidCredentialsException("Email or password is incorrect.");
                }
            }
        }

        // 3. Fallback dummy hash comparison to mitigate timing attacks
        passwordEncoder.matches(request.password(), DUMMY_BCRYPT_HASH);
        throw new InvalidCredentialsException("Email or password is incorrect.");
    }
}
