package io.routalyx.fleet.persistence;

import io.routalyx.fleet.VehicleStatus;
import io.routalyx.fleet.application.VehicleCommand;
import io.routalyx.fleet.application.VehiclePageResult;
import io.routalyx.fleet.application.VehicleResult;
import io.routalyx.fleet.application.VehicleStore;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class VehicleRepositoryAdapter implements VehicleStore {

    private final VehicleRepository vehicleRepository;

    public VehicleRepositoryAdapter(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    public Optional<VehicleResult> findById(UUID id) {
        return vehicleRepository.findById(id)
                .map(this::toResult);
    }

    @Override
    public Optional<VehicleResult> findByLicensePlate(
            String licensePlate
    ) {
        return vehicleRepository.findByLicensePlate(licensePlate)
                .map(this::toResult);
    }

    @Override
    public boolean existsByLicensePlate(String licensePlate) {
        return vehicleRepository.existsByLicensePlate(licensePlate);
    }

    @Override
    public VehicleResult createVehicle(VehicleCommand command) {
        VehicleEntity entity = new VehicleEntity(
                UUID.randomUUID(),
                command.licensePlate(),
                command.status(),
                command.activeSafetyIssue()
        );

        return toResult(vehicleRepository.save(entity));
    }

    @Override
    public Optional<VehicleResult> updateVehicle(
            UUID id,
            VehicleCommand command
    ) {
        return vehicleRepository.findById(id)
                .map(entity -> {
                    entity.update(
                            command.licensePlate(),
                            command.status(),
                            command.activeSafetyIssue()
                    );

                    return vehicleRepository.save(entity);
                })
                .map(this::toResult);
    }
    @Override
    public void deleteVehicle(UUID id) {
        vehicleRepository.deleteById(id);
    }

    @Override
    public VehiclePageResult findVehicles(
            VehicleStatus status,
            int page,
            int size
    ) {
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

    private VehicleResult toResult(VehicleEntity entity) {
        return new VehicleResult(
                entity.getId(),
                entity.getLicensePlate(),
                entity.getStatus(),
                entity.isActiveSafetyIssue(),
                entity.getCreatedAt()
        );
    }
}
