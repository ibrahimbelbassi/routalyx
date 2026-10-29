package io.routalyx.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Request validation failed");

        return ResponseEntity.badRequest()
                .body(Map.of("error", message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error",
                        "The request conflicts with existing database data"
                ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(
            ResponseStatusException exception
    ) {
        HttpStatusCode status = exception.getStatusCode();

        String message = exception.getReason() != null
                ? exception.getReason()
                : "Request could not be processed";

        return ResponseEntity.status(status)
                .body(Map.of("error", message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error",
                        "Invalid value for parameter: " + exception.getName()
                ));
    }
}
