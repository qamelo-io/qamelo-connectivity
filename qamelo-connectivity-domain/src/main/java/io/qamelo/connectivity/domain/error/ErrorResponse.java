package io.qamelo.connectivity.domain.error;

public record ErrorResponse(String error, String message, int status) {
}
