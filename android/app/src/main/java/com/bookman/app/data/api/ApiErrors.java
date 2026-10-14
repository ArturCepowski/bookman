package com.bookman.app.data.api;

import com.bookman.app.common.AppError;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import okhttp3.ResponseBody;
import retrofit2.Response;

/** Translates backend error responses ({"code": ...}) into {@link AppError}. */
public final class ApiErrors {

    private static final Gson GSON = new Gson();

    private ApiErrors() {
    }

    public static AppError from(Response<?> response) {
        String code = codeOf(response);
        if (code == null) {
            return response.code() == 401 ? AppError.SESSION_EXPIRED : AppError.UNKNOWN;
        }
        switch (code) {
            case "VALIDATION_FAILED":
                return AppError.VALIDATION;
            case "EMAIL_TAKEN":
                return AppError.EMAIL_TAKEN;
            case "INVALID_CREDENTIALS":
                return AppError.INVALID_CREDENTIALS;
            case "UNAUTHORIZED":
                return AppError.SESSION_EXPIRED;
            case "FORBIDDEN":
                return AppError.FORBIDDEN;
            case "ICAL_UNREACHABLE":
                return AppError.ICAL_UNREACHABLE;
            case "ICAL_NOT_A_CALENDAR":
                return AppError.ICAL_NOT_A_CALENDAR;
            default:
                return AppError.UNKNOWN;
        }
    }

    private static String codeOf(Response<?> response) {
        try (ResponseBody body = response.errorBody()) {
            if (body == null) {
                return null;
            }
            ApiErrorDto dto = GSON.fromJson(body.charStream(), ApiErrorDto.class);
            return dto == null ? null : dto.code;
        } catch (JsonParseException | IllegalStateException e) {
            return null;
        }
    }

    public static AppError from(IOException e) {
        return AppError.NETWORK;
    }
}
