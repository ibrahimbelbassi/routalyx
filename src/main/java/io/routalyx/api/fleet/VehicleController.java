package io.routalyx.api.fleet;

import io.routalyx.fleet.VehicleStatus;
import io.routalyx.fleet.application.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public VehiclePageResponse getAllVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) VehicleStatus status
    ) {
        return vehicleService.getVehicles(status, page, size);
    }

    @GetMapping("/{id}")
    public VehicleResponse getVehicleById(@PathVariable UUID id) {
        return vehicleService.getVehicleById(id);
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(
            @Valid @RequestBody VehicleRequest request
    ) {
        VehicleResponse createdVehicle = vehicleService.createVehicle(request);

        URI location = URI.create(
                "/api/v1/vehicles/" + createdVehicle.id()
        );

        return ResponseEntity.created(location).body(createdVehicle);
    }

    @PutMapping("/{id}")
    public VehicleResponse updateVehicle(
            @PathVariable UUID id,
            @Valid @RequestBody VehicleRequest request
    ) {
        return vehicleService.updateVehicle(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable UUID id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}