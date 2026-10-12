package io.routalyx.api.system;

public record SystemStatusResponse (
        String application,
        String status,
        String version
) {
}
