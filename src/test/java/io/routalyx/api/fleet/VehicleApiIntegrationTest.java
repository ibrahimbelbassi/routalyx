package io.routalyx.api.fleet;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import io.routalyx.fleet.VehicleStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VehicleApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateReadUpdateAndDeleteVehicle() throws Exception {
        String licensePlate = uniqueLicensePlate();

        VehicleRequest createRequest = new VehicleRequest(
                licensePlate,
                VehicleStatus.AVAILABLE,
                false
        );

        String createResponse = mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.licensePlate").value(licensePlate))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdJson = objectMapper.readTree(createResponse);
        UUID id = UUID.fromString(createdJson.get("id").asText());

        mockMvc.perform(get("/api/v1/vehicles/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.licensePlate").value(licensePlate));

        VehicleRequest updateRequest = new VehicleRequest(
                licensePlate,
                VehicleStatus.IN_MAINTENANCE,
                true
        );

        mockMvc.perform(put("/api/v1/vehicles/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_MAINTENANCE"))
                .andExpect(jsonPath("$.activeSafetyIssue").value(true));

        mockMvc.perform(get("/api/v1/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + id + "')]").isNotEmpty());

        mockMvc.perform(delete("/api/v1/vehicles/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/vehicles/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectDuplicateLicensePlate() throws Exception {
        String licensePlate = uniqueLicensePlate();

        VehicleRequest request = new VehicleRequest(
                licensePlate,
                VehicleStatus.AVAILABLE,
                false
        );

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldRejectBlankLicensePlate() throws Exception {
        String invalidRequest = """
                {
                  "licensePlate": "   ",
                  "status": "AVAILABLE",
                  "activeSafetyIssue": false
                }
                """;

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturnNotFoundForUnknownVehicle() throws Exception {
        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/vehicles/{id}", unknownId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectLicensePlateLongerThanTwentyCharacters() throws Exception {
        String invalidRequest = """
            {
              "licensePlate": "ABCDEFGHIJKLMNOPQRSTU",
              "status": "AVAILABLE",
              "activeSafetyIssue": false
            }
            """;

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingStatus() throws Exception {
        String invalidRequest = """
            {
              "licensePlate": "T-VALIDATION-01",
              "activeSafetyIssue": false
            }
            """;

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingUnknownVehicle() throws Exception {
        UUID unknownId = UUID.randomUUID();

        VehicleRequest request = new VehicleRequest(
                uniqueLicensePlate(),
                VehicleStatus.AVAILABLE,
                false
        );

        mockMvc.perform(put("/api/v1/vehicles/{id}", unknownId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundWhenDeletingUnknownVehicle() throws Exception {
        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/vehicles/{id}", unknownId))
                .andExpect(status().isNotFound());
    }

    private String uniqueLicensePlate() {
        return "T-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 15);
    }
}