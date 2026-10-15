package com.bookman.auth;

import com.bookman.common.ApiException;
import com.bookman.common.ErrorCode;
import com.bookman.security.JwtService;
import com.bookman.user.User;
import com.bookman.user.UserRepository;
import com.bookman.user.UserResponse;
import java.time.Clock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw emailTaken();
        }
        User user = new User(email, passwordEncoder.encode(request.password()), request.phoneNumber(),
                request.language(), clock.instant());
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Concurrent registration with the same e-mail hit the unique index.
            throw emailTaken();
        }
        return new AuthResponse(jwtService.issueToken(user.getId()), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(User.normalizeEmail(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS,
                        "Invalid e-mail or password"));
        return new AuthResponse(jwtService.issueToken(user.getId()), UserResponse.from(user));
    }

    private static ApiException emailTaken() {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.EMAIL_TAKEN, "E-mail is already registered");
    }
}
