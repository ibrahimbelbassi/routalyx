
package io.routalyx.fleet.persistence;

import io.routalyx.fleet.VehicleStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class VehicleRepositoryTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    void shouldSaveAndFindVehicle() {
        UUID id = UUID.randomUUID();
        String licensePlate = "TEST-" + UUID.randomUUID()
                .toString().substring(0, 8);

        VehicleEntity vehicle = new VehicleEntity(
                id,
                licensePlate,
                VehicleStatus.AVAILABLE,
                false
        );

        VehicleEntity savedVehicle = vehicleRepository.save(vehicle);

        assertEquals(id, savedVehicle.getId());

        var foundVehicle = vehicleRepository.findById(id);

        assertTrue(foundVehicle.isPresent());
        assertEquals(licensePlate, foundVehicle.get().getLicensePlate());
        assertEquals(VehicleStatus.AVAILABLE, foundVehicle.get().getStatus());
        assertFalse(foundVehicle.get().isActiveSafetyIssue());
    }

    @Test
    void shouldFindVehicleByLicensePlate() {
        UUID id = UUID.randomUUID();
        String licensePlate = "TEST-" + UUID.randomUUID()
                .toString().substring(0, 8);

        VehicleEntity vehicle = new VehicleEntity(
                id,
                licensePlate,
                VehicleStatus.AVAILABLE,
                false
        );

        vehicleRepository.save(vehicle);

        var foundVehicle =
                vehicleRepository.findByLicensePlate(licensePlate);

        assertTrue(foundVehicle.isPresent());
        assertEquals(id, foundVehicle.get().getId());
    }
}
