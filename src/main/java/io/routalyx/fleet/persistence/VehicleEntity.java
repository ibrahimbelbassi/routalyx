
package io.routalyx.fleet.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.routalyx.fleet.VehicleStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
public class VehicleEntity {

    @Id
    private UUID id;

    @Column(name = "license_plate", nullable = false, unique = true, length = 20)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VehicleStatus status;

    @Column(name = "active_safety_issue", nullable = false)
    private boolean activeSafetyIssue;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected VehicleEntity() {
        // Wird von JPA benötigt.
    }

    public VehicleEntity(
            UUID id,
            String licensePlate,
            VehicleStatus status,
            boolean activeSafetyIssue
    ) {
        this.id = id;
        this.licensePlate = licensePlate;
        this.status = status;
        this.activeSafetyIssue = activeSafetyIssue;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public boolean isActiveSafetyIssue() {
        return activeSafetyIssue;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
