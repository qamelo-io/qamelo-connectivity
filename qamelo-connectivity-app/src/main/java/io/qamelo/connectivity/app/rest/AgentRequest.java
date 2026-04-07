package io.qamelo.connectivity.app.rest;

public record AgentRequest(String name, String description, String k8sNamespace) {}
