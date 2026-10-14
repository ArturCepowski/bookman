package com.bookman.user;

import com.bookman.common.ApiException;
import com.bookman.common.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(long userId) {
        return UserResponse.from(load(userId));
    }

    @Transactional
    public UserResponse updateProfile(long userId, UpdateProfileRequest request) {
        User user = load(userId);
        user.setPhoneNumber(request.phoneNumber());
        user.setLanguage(request.language());
        return UserResponse.from(user);
    }

    User load(long userId) {
        // A valid token for a user that no longer exists is treated as an expired session.
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Unknown user"));
    }
}
