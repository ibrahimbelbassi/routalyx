package io.routalyx.api.fleet;

import java.util.List;

public record VehiclePageResponse(
        List<VehicleResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}