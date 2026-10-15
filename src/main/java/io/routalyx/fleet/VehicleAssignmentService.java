package io.routalyx.fleet;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VehicleAssignmentService {

    public VehicleAssignmentResult assignVehicle(Vehicle vehicle, Tour tour) {
        if (vehicle.activeSafetyIssue()) {
            throw new VehicleSafetyException(
                    "Vehicle cannot be assigned because it has an active safety issue."
            );
        }

        if (vehicle.status() != VehicleStatus.AVAILABLE) {
            throw new IllegalStateException("Vehicle is not available for assignment.");
        }

        Vehicle assignedVehicle = new Vehicle(
                vehicle.id(),
                vehicle.licensePlate(),
                VehicleStatus.ASSIGNED,
                false
        );

        VehicleAssignment assignment = new VehicleAssignment(
                UUID.randomUUID(),
                assignedVehicle.id(),
                tour.id()
        );

        return new VehicleAssignmentResult(assignedVehicle, assignment);
    }
}
