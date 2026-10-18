package io.routalyx.fleet.application;

import io.routalyx.api.fleet.VehicleResponse;
import io.routalyx.fleet.persistence.VehicleEntity;
import io.routalyx.fleet.persistence.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
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
