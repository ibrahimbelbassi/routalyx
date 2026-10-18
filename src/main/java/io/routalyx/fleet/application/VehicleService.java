package io.routalyx.fleet.application;

import io.routalyx.api.fleet.VehicleResponse;
import io.routalyx.fleet.persistence.VehicleEntity;
import io.routalyx.fleet.persistence.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
