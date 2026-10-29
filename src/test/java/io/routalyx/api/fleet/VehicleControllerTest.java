package io.routalyx.api.fleet;

import io.routalyx.fleet.VehicleStatus;
import io.routalyx.fleet.application.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehicleController.class)
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleService vehicleService;


    @Test
    void shouldReturnAllVehicles() throws Exception {
        UUID vehicleId = UUID.fromString(
                "929819c3-7d0b-46c1-9efb-5ff809221953"
        );

        OffsetDateTime createdAt =
                OffsetDateTime.parse("2026-10-17T22:56:00Z");

        VehiclePageResponse response = new VehiclePageResponse(
                List.of(new VehicleResponse(
                        vehicleId,
                        "DBTEST-001",
                        VehicleStatus.AVAILABLE,
                        false,
                        createdAt
                )),
                0,
                20,
                1,
                1,
                true,
                true
        );

        given(vehicleService.getVehicles(null, 0, 20))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/vehicles"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(vehicleId.toString()))
                .andExpect(jsonPath("$.content[0].licensePlate").value("DBTEST-001"))
                .andExpect(jsonPath("$.content[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.content[0].activeSafetyIssue").value(false))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void shouldReturnVehicleById() throws Exception {
        UUID vehicleId = UUID.fromString(
                "929819c3-7d0b-46c1-9efb-5ff809221953"
        );

        OffsetDateTime createdAt =
                OffsetDateTime.parse("2026-10-17T22:56:00Z");

        VehicleResponse response = new VehicleResponse(
                vehicleId,
                "DBTEST-001",
                VehicleStatus.AVAILABLE,
                false,
                createdAt
        );

        given(vehicleService.getVehicleById(vehicleId))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/vehicles/{id}", vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vehicleId.toString()))
                .andExpect(jsonPath("$.licensePlate").value("DBTEST-001"));
    }

    @Test
    void shouldReturnNotFoundForUnknownVehicle() throws Exception {
        UUID unknownId = UUID.randomUUID();

        given(vehicleService.getVehicleById(unknownId))
                .willThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Vehicle not found"
                ));

        mockMvc.perform(get("/api/v1/vehicles/{id}", unknownId))
                .andExpect(status().isNotFound());
    }
}
