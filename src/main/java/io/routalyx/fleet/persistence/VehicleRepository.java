package io.routalyx.fleet.persistence;

import io.routalyx.fleet.VehicleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository <VehicleEntity, UUID> {
    Optional<VehicleEntity> findByLicensePlate(String licensePlate);
    boolean existsByLicensePlate(String licensePlate);
    Page<VehicleEntity> findByStatus(VehicleStatus status, Pageable pageable);
}
