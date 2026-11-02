package io.routalyx.fleet.application;

import io.routalyx.fleet.VehicleStatus;
import io.routalyx.fleet.application.exception.DuplicateLicensePlateException;
import io.routalyx.fleet.application.exception.VehicleNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleStore vehicleStore;

    @InjectMocks
    private VehicleService vehicleService;

    private UUID vehicleId;
    private VehicleCommand command;
    private VehicleResult vehicleResult;

    @BeforeEach
    void setUp() {
        vehicleId = UUID.randomUUID();

        command = new VehicleCommand(
                "SB-IB-92",
                VehicleStatus.AVAILABLE,
                false
        );

        vehicleResult = new VehicleResult(
                vehicleId,
                command.licensePlate(),
                command.status(),
                command.activeSafetyIssue(),
                OffsetDateTime.now()
        );
    }

    @Test
    void shouldReturnVehicleWhenFoundById() {
        when(vehicleStore.findById(vehicleId))
                .thenReturn(Optional.of(vehicleResult));

        VehicleResult result = vehicleService.getVehicleById(vehicleId);

        assertEquals(vehicleId, result.id());
        assertEquals("SB-IB-92", result.licensePlate());

        verify(vehicleStore).findById(vehicleId);
    }

    @Test
    void shouldThrowWhenVehicleNotFoundById() {
        when(vehicleStore.findById(vehicleId))
                .thenReturn(Optional.empty());

        assertThrows(
                VehicleNotFoundException.class,
                () -> vehicleService.getVehicleById(vehicleId)
        );
    }

    @Test
    void shouldCreateVehicleWhenLicensePlateIsAvailable() {
        when(vehicleStore.existsByLicensePlate(command.licensePlate()))
                .thenReturn(false);

        when(vehicleStore.createVehicle(command))
                .thenReturn(vehicleResult);

        VehicleResult result = vehicleService.createVehicle(command);

        assertEquals(vehicleId, result.id());
        assertEquals(command.licensePlate(), result.licensePlate());

        verify(vehicleStore).createVehicle(command);
    }

    @Test
    void shouldRejectCreationWhenLicensePlateAlreadyExists() {
        when(vehicleStore.existsByLicensePlate(command.licensePlate()))
                .thenReturn(true);

        assertThrows(
                DuplicateLicensePlateException.class,
                () -> vehicleService.createVehicle(command)
        );

        verify(vehicleStore, never()).createVehicle(any());
    }

    @Test
    void shouldUpdateVehicleWhenValid() {
        when(vehicleStore.findById(vehicleId))
                .thenReturn(Optional.of(vehicleResult));

        when(vehicleStore.findByLicensePlate(command.licensePlate()))
                .thenReturn(Optional.of(vehicleResult));

        when(vehicleStore.updateVehicle(vehicleId, command))
                .thenReturn(Optional.of(vehicleResult));

        VehicleResult result =
                vehicleService.updateVehicle(vehicleId, command);

        assertEquals(vehicleId, result.id());

        verify(vehicleStore).updateVehicle(vehicleId, command);
    }

    @Test
    void shouldThrowWhenUpdatingVehicleThatDoesNotExist() {
        when(vehicleStore.findById(vehicleId))
                .thenReturn(Optional.empty());

        assertThrows(
                VehicleNotFoundException.class,
                () -> vehicleService.updateVehicle(vehicleId, command)
        );

        verify(vehicleStore, never()).updateVehicle(any(), any());
    }

    @Test
    void shouldRejectUpdateWhenLicensePlateBelongsToAnotherVehicle() {
        UUID anotherVehicleId = UUID.randomUUID();

        VehicleResult anotherVehicle = new VehicleResult(
                anotherVehicleId,
                command.licensePlate(),
                VehicleStatus.AVAILABLE,
                false,
                OffsetDateTime.now()
        );

        when(vehicleStore.findById(vehicleId))
                .thenReturn(Optional.of(vehicleResult));

        when(vehicleStore.findByLicensePlate(command.licensePlate()))
                .thenReturn(Optional.of(anotherVehicle));

        assertThrows(
                DuplicateLicensePlateException.class,
                () -> vehicleService.updateVehicle(vehicleId, command)
        );

        verify(vehicleStore, never()).updateVehicle(any(), any());
    }

    @Test
    void shouldThrowWhenVehicleDisappearsDuringUpdate() {
        when(vehicleStore.findById(vehicleId))
                .thenReturn(Optional.of(vehicleResult));

        when(vehicleStore.findByLicensePlate(command.licensePlate()))
                .thenReturn(Optional.empty());

        when(vehicleStore.updateVehicle(vehicleId, command))
                .thenReturn(Optional.empty());

        assertThrows(
                VehicleNotFoundException.class,
                () -> vehicleService.updateVehicle(vehicleId, command)
        );
    }

    @Test
    void shouldDeleteVehicleWhenFound() {
        when(vehicleStore.findById(vehicleId))
                .thenReturn(Optional.of(vehicleResult));

        vehicleService.deleteVehicle(vehicleId);

        verify(vehicleStore).deleteVehicle(vehicleId);
    }

    @Test
    void shouldThrowWhenDeletingVehicleThatDoesNotExist() {
        when(vehicleStore.findById(vehicleId))
                .thenReturn(Optional.empty());

        assertThrows(
                VehicleNotFoundException.class,
                () -> vehicleService.deleteVehicle(vehicleId)
        );

        verify(vehicleStore, never()).deleteVehicle(any());
    }
}
