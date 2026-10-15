package io.routalyx.fleet;

import java.util.UUID;

public record VehicleAssignment(
        UUID id,
        UUID vehicleId,
        UUID tourId
) {
}
