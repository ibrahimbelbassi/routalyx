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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.net.URI;
import java.util.UUID;

@Tag(
        name = "Vehicles",
        description = "Manage fleet vehicles"
)

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @Operation(
            summary = "List vehicles",
            description = "Returns a paginated list of vehicles, optionally filtered by status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicles retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or status parameter")
    })
    @GetMapping
    public VehiclePageResponse getAllVehicles(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of vehicles per page", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Optional vehicle status filter")
            @RequestParam(required = false) VehicleStatus status
    ) {
        return vehicleService.getVehicles(status, page, size);
    }


    @Operation(
            summary = "Get vehicle by ID",
            description = "Returns a single vehicle identified by its UUID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    @GetMapping("/{id}")
    public VehicleResponse getVehicleById(
            @Parameter(description = "UUID of the vehicle", required = true)
            @PathVariable UUID id
    ) {
        return vehicleService.getVehicleById(id);
    }

    @Operation(
            summary = "Create vehicle",
            description = "Creates a vehicle and returns its representation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vehicle created successfully"),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "409", description = "Conflict with existing database data")
    })
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

    @Operation(
            summary = "Update vehicle",
            description = "Updates an existing vehicle identified by its UUID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid UUID or request validation failed"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found"),
            @ApiResponse(responseCode = "409", description = "Conflict with existing database data")
    })
    @PutMapping("/{id}")
    public VehicleResponse updateVehicle(
            @PathVariable UUID id,
            @Valid @RequestBody VehicleRequest request
    ) {
        return vehicleService.updateVehicle(id, request);
    }

    @Operation(
            summary = "Delete vehicle",
            description = "Deletes a vehicle identified by its UUID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vehicle deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable UUID id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}