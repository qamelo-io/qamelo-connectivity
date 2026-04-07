package io.qamelo.connectivity.app.rest.internal;

import java.util.Map;

public record AgreementMatchRequest(String channelType, Map<String, String> criteria) {}
