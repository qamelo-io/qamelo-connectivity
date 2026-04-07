package io.qamelo.connectivity.app.security.iam;

import java.util.List;

public record IamAuthContextResponse(
        String userId,
        List<String> permissions,
        List<String> roles
) {}
