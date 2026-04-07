package io.qamelo.connectivity.app.rest;

import java.util.Map;

public record ChannelRequest(
        String name,
        String partnerId,
        String connectionId,
        String type,
        String direction,
        Map<String, String> properties,
        Boolean enabled,
        String description,
        String status
) {}
