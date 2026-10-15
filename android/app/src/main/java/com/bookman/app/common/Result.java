package com.bookman.app.common;

import androidx.annotation.Nullable;

public final class Result<T> {

    @Nullable private final T value;
    @Nullable private final AppError error;

    private Result(@Nullable T value, @Nullable AppError error) {
        this.value = value;
        this.error = error;
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(value, null);
    }

    public static <T> Result<T> failure(AppError error) {
        return new Result<>(null, error);
    }

    public boolean isSuccess() {
        return error == null;
    }

    @Nullable
    public T value() {
        return value;
    }

    @Nullable
    public AppError error() {
        return error;
    }
}
