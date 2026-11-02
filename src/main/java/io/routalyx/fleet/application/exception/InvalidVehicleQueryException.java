package io.routalyx.fleet.application.exception;

public class InvalidVehicleQueryException extends RuntimeException {
    public InvalidVehicleQueryException(String message) {
        super(message);
    }
}
