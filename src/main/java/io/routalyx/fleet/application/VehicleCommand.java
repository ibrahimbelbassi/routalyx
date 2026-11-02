package io.routalyx.fleet.application;

import io.routalyx.fleet.VehicleStatus;

public record VehicleCommand(
        String licensePlate,
        VehicleStatus status,
        boolean activeSafetyIssue
) {
}