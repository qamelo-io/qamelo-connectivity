package io.qamelo.connectivity.app.security.auth;

import java.util.Set;

public record UserAuthContext(String userId, Set<String> permissions) {}
