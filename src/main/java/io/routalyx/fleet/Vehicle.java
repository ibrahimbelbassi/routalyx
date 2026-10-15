package io.routalyx.fleet;

import java.util.UUID;

public record Vehicle(
        UUID id,
        String licensePlate,
        VehicleStatus status,
        boolean activeSafetyIssue
) {
}
