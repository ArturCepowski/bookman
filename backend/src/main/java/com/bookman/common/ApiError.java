package com.bookman.common;

import java.util.Map;

public record ApiError(ErrorCode code, String message, Map<String, String> fieldErrors) {

    public static ApiError of(ErrorCode code, String message) {
        return new ApiError(code, message, Map.of());
    }
}
