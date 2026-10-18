package io.routalyx.api.fleet;

import io.routalyx.fleet.VehicleStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleResponse(
        UUID id,
        String licensePlate,
        VehicleStatus status,
        boolean activeSafetyIssue,
        OffsetDateTime createdAt
) {
}

