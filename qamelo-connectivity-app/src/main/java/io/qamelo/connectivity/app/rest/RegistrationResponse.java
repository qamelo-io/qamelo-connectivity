package io.qamelo.connectivity.app.rest;

public record RegistrationResponse(String certificate, String caChain, String gatewayUrl) {}
