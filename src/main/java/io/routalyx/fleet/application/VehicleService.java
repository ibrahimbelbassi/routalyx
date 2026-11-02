package io.routalyx.fleet.application;

import io.routalyx.fleet.VehicleStatus;
import io.routalyx.fleet.persistence.VehicleEntity;
import io.routalyx.fleet.persistence.VehicleRepository;
import io.routalyx.fleet.application.exception.DuplicateLicensePlateException;
import io.routalyx.fleet.application.exception.InvalidVehicleQueryException;
import io.routalyx.fleet.application.exception.VehicleNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public VehicleResult getVehicleById(UUID id) {
        return vehicleRepository.findById(id)
                .map(this::toResult)
                .orElseThrow(() -> new VehicleNotFoundException(
                        "Vehicle not found"
                ));
    }

    @Transactional
    public VehicleResult createVehicle(VehicleCommand command) {
        if (vehicleRepository.existsByLicensePlate(command.licensePlate())) {
            throw new DuplicateLicensePlateException(
                    "A vehicle with this license plate already exists"
            );
        }

        VehicleEntity entity = new VehicleEntity(
                UUID.randomUUID(),
                command.licensePlate(),
                command.status(),
                command.activeSafetyIssue()
        );

        return toResult(vehicleRepository.save(entity));
    }

    @Transactional
    public VehicleResult updateVehicle(UUID id, VehicleCommand command) {
        VehicleEntity entity = vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(
                        "Vehicle not found"
                ));

        vehicleRepository.findByLicensePlate(command.licensePlate())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateLicensePlateException(
                            "A vehicle with this license plate already exists"
                    );
                });

        entity.update(
                command.licensePlate(),
                command.status(),
                command.activeSafetyIssue()
        );

        return toResult(vehicleRepository.save(entity));
    }

    @Transactional
    public void deleteVehicle(UUID id) {
        VehicleEntity entity = vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(
                        "Vehicle not found"
                ));

        vehicleRepository.delete(entity);
    }

    private VehicleResult toResult(VehicleEntity entity) {
        return new VehicleResult(
                entity.getId(),
                entity.getLicensePlate(),
                entity.getStatus(),
                entity.isActiveSafetyIssue(),
                entity.getCreatedAt()
        );
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

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("licensePlate"),
                        Sort.Order.asc("id")
                )
        );

        Page<VehicleEntity> vehiclePage = status == null
                ? vehicleRepository.findAll(pageable)
                : vehicleRepository.findByStatus(status, pageable);

        return new VehiclePageResult(
                vehiclePage.getContent()
                        .stream()
                        .map(this::toResult)
                        .toList(),
                vehiclePage.getNumber(),
                vehiclePage.getSize(),
                vehiclePage.getTotalElements(),
                vehiclePage.getTotalPages(),
                vehiclePage.isFirst(),
                vehiclePage.isLast()
        );
    }
}
