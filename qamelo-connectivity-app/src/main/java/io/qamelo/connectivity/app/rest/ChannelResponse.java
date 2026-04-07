package io.qamelo.connectivity.app.rest;

import java.util.Map;
import java.util.UUID;

public record ChannelResponse(
        UUID id,
        String name,
        UUID partnerId,
        UUID connectionId,
        String type,
        String direction,
        Map<String, String> properties,
        boolean enabled,
        String createdAt,
        String createdBy,
        String modifiedAt,
        String modifiedBy
) {}
