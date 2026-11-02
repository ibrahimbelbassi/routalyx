package io.routalyx.fleet.application;

import io.routalyx.fleet.VehicleStatus;
import io.routalyx.fleet.application.exception.DuplicateLicensePlateException;
import io.routalyx.fleet.application.exception.InvalidVehicleQueryException;
import io.routalyx.fleet.application.exception.VehicleNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class VehicleService {

    private final VehicleStore vehicleStore;

    public VehicleService(VehicleStore vehicleStore) {
        this.vehicleStore = vehicleStore;
    }

    public VehicleResult getVehicleById(UUID id) {
        return vehicleStore.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(
                        "Vehicle not found"
                ));
    }

    @Transactional
    public VehicleResult createVehicle(VehicleCommand command) {
        if (vehicleStore.existsByLicensePlate(command.licensePlate())) {
            throw new DuplicateLicensePlateException(
                    "A vehicle with this license plate already exists"
            );
        }

        return vehicleStore.createVehicle(command);
    }

    @Transactional
    public VehicleResult updateVehicle(UUID id, VehicleCommand command) {
        vehicleStore.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(
                        "Vehicle not found"
                ));

        vehicleStore.findByLicensePlate(command.licensePlate())
                .filter(existing -> !existing.id().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateLicensePlateException(
                            "A vehicle with this license plate already exists"
                    );
                });

        return vehicleStore.updateVehicle(id, command)
                .orElseThrow(() -> new VehicleNotFoundException(
                        "Vehicle not found"
                ));
    }

    @Transactional
    public void deleteVehicle(UUID id) {
        vehicleStore.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(
                        "Vehicle not found"
                ));

        vehicleStore.deleteVehicle(id);
    }

    public VehiclePageResult getVehicles(
            VehicleStatus status,
            int page,
            int size
    ) {
        if (page < 0) {
            throw new InvalidVehicleQueryException(
                    "Page must be greater than or equal to 0"
            );
        }

        if (size < 1 || size > 100) {
            throw new InvalidVehicleQueryException(
                    "Size must be between 1 and 100"
            );
        }

        return vehicleStore.findVehicles(status, page, size);
    }
}
