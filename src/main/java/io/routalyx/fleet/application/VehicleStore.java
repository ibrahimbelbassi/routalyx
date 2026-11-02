package io.routalyx.fleet.application;

import io.routalyx.fleet.VehicleStatus;

import java.util.Optional;
import java.util.UUID;

public interface VehicleStore {

    Optional<VehicleResult> findById(UUID id);

    Optional<VehicleResult> findByLicensePlate(String licensePlate);

    boolean existsByLicensePlate(String licensePlate);

    VehicleResult createVehicle(VehicleCommand command);

    Optional<VehicleResult> updateVehicle(UUID id, VehicleCommand command);

    void deleteVehicle(UUID id);

    VehiclePageResult findVehicles(
            VehicleStatus status,
            int page,
            int size
    );
}
