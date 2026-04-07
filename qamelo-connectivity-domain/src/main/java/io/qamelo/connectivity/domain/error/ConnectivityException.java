package io.qamelo.connectivity.domain.error;

public class ConnectivityException extends RuntimeException {

    private final ConnectivityErrorCode code;

    public ConnectivityException(ConnectivityErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ConnectivityException(ConnectivityErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ConnectivityErrorCode code() {
        return code;
    }
}
