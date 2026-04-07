package io.qamelo.connectivity.app.error;

import io.qamelo.connectivity.domain.error.ConnectivityException;
import io.qamelo.connectivity.domain.error.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ConnectivityExceptionMapper implements ExceptionMapper<ConnectivityException> {

    @Override
    public Response toResponse(ConnectivityException exception) {
        int status = mapStatus(exception);
        return Response.status(status)
                .entity(new ErrorResponse(exception.code().name(), exception.getMessage(), status))
                .build();
    }

    private int mapStatus(ConnectivityException exception) {
        return switch (exception.code()) {
            case NOT_FOUND -> 404;
            case BAD_REQUEST -> 400;
            case CONFLICT -> 409;
            case FORBIDDEN -> 403;
            case UNAUTHORIZED -> 401;
            case SERVICE_UNAVAILABLE -> 503;
            case INTERNAL_ERROR -> 500;
        };
    }
}
