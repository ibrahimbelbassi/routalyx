package io.routalyx.api.fleet;

import io.routalyx.fleet.VehicleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VehicleRequest(
        @NotBlank
        @Size(max = 20)
        String licensePlate,

        @NotNull
        VehicleStatus status,

        boolean activeSafetyIssue
) {
}