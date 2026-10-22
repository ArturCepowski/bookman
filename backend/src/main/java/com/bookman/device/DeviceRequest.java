package com.bookman.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceRequest(@NotBlank @Size(max = 4096) String token) {
}
