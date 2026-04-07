package io.qamelo.connectivity.domain.agent;

import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.UUID;

/**
 * SPI for managing K8s Services that back virtual host routing.
 * Implementations live in the infra module.
 */
public interface KubernetesServiceManager {

    Uni<Void> createService(String namespace, String serviceName, UUID agentId,
                            UUID virtualHostId, String gatewayIp, int targetPort);

    Uni<Void> updateEndpoints(String namespace, String serviceName,
                              List<String> gatewayIps, int targetPort);

    Uni<Void> deleteService(String namespace, String serviceName);

    Uni<List<String>> listManagedServiceNames(String namespace);
}
