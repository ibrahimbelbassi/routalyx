package io.routalyx.fleet.application;

import io.routalyx.api.fleet.VehicleRequest;
import io.routalyx.api.fleet.VehicleResponse;
import io.routalyx.fleet.persistence.VehicleEntity;
import io.routalyx.fleet.persistence.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public VehicleResponse getVehicleById(UUID id) {
        return vehicleRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Vehicle not found"
                ));
    }

    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request) {
        if (vehicleRepository.existsByLicensePlate(request.licensePlate())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A vehicle with this license plate already exists"
            );
        }

        VehicleEntity entity = new VehicleEntity(
                UUID.randomUUID(),
                request.licensePlate(),
                request.status(),
                request.activeSafetyIssue()
        );

        return toResponse(vehicleRepository.save(entity));
    }

    @Transactional
    public VehicleResponse updateVehicle(UUID id, VehicleRequest request) {
        VehicleEntity entity = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Vehicle not found"
                ));

        vehicleRepository.findByLicensePlate(request.licensePlate())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "A vehicle with this license plate already exists"
                    );
                });

        entity.update(
                request.licensePlate(),
                request.status(),
                request.activeSafetyIssue()
        );

        return toResponse(vehicleRepository.save(entity));
    }

    @Transactional
    public void deleteVehicle(UUID id) {
        VehicleEntity entity = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Vehicle not found"
                ));

        vehicleRepository.delete(entity);
    }

    private VehicleResponse toResponse(VehicleEntity entity) {
        return new VehicleResponse(
                entity.getId(),
                entity.getLicensePlate(),
                entity.getStatus(),
                entity.isActiveSafetyIssue(),
                entity.getCreatedAt()
        );
    }
}
