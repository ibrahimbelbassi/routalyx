package io.routalyx.fleet.application;

import java.util.List;

public record VehiclePageResult(
        List<VehicleResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}