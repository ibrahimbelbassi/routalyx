package io.routalyx.fleet;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VehicleAssignmentServiceTest {
    private final VehicleAssignmentService service = new VehicleAssignmentService();

    @Test
    public void shouldRejectVehicleWithActiveSafetyIssue() {
        Vehicle vehicle = new Vehicle(
                UUID.randomUUID(),
                "SB-IB-92",
                VehicleStatus.AVAILABLE,
                true
        );

        assertThrows(VehicleSafetyException.class, () -> service.assignVehicle(vehicle, new Tour(UUID.randomUUID(), "TOUR-2026-001")));
    }

    @Test
    public void shouldRejectUnavailableVehicle() {
        Vehicle vehicle = new Vehicle(
                UUID.randomUUID(),
                "SB-IB-92",
                VehicleStatus.ASSIGNED,
                false
        );

        assertThrows(IllegalStateException.class, () -> service.assignVehicle(vehicle, new Tour(UUID.randomUUID(), "TOUR-2026-001")));
    }


    @Test
    void shouldAssignAvailableVehicleToTour() {
        Vehicle vehicle = new Vehicle(
                UUID.randomUUID(),
                "SB-XY-123",
                VehicleStatus.AVAILABLE,
                false
        );

        Tour tour = new Tour(
                UUID.randomUUID(),
                "TOUR-2026-001"
        );

        VehicleAssignmentResult result =
                service.assignVehicle(vehicle, tour);

        assertEquals(
                VehicleStatus.ASSIGNED,
                result.vehicle().status()
        );

        assertEquals(
                vehicle.id(),
                result.assignment().vehicleId()
        );

        assertEquals(
                tour.id(),
                result.assignment().tourId()
        );

        assertEquals(
                VehicleStatus.AVAILABLE,
                vehicle.status()
        );
    }
}
