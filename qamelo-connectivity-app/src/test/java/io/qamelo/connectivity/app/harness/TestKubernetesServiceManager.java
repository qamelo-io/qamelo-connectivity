package io.qamelo.connectivity.app.harness;

import io.qamelo.connectivity.domain.agent.KubernetesServiceManager;
import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory KubernetesServiceManager for integration tests.
 * Records created/deleted services for assertion in tests.
 */
@Mock
@ApplicationScoped
public class TestKubernetesServiceManager implements KubernetesServiceManager {

    public record ServiceRecord(String namespace, String serviceName, UUID agentId,
                                UUID virtualHostId, List<String> gatewayIps, int targetPort) {}

    private final ConcurrentHashMap<String, ServiceRecord> services = new ConcurrentHashMap<>();

    @Override
    public Uni<Void> createService(String namespace, String serviceName, UUID agentId,
                                   UUID virtualHostId, String gatewayIp, int targetPort) {
        services.put(key(namespace, serviceName),
                new ServiceRecord(namespace, serviceName, agentId, virtualHostId, List.of(gatewayIp), targetPort));
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Void> updateEndpoints(String namespace, String serviceName,
                                     List<String> gatewayIps, int targetPort) {
        String k = key(namespace, serviceName);
        ServiceRecord existing = services.get(k);
        if (existing != null) {
            services.put(k, new ServiceRecord(namespace, serviceName, existing.agentId(),
                    existing.virtualHostId(), gatewayIps, targetPort));
        }
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Void> deleteService(String namespace, String serviceName) {
        services.remove(key(namespace, serviceName));
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<List<String>> listManagedServiceNames(String namespace) {
        List<String> names = services.values().stream()
                .filter(r -> r.namespace().equals(namespace))
                .map(ServiceRecord::serviceName)
                .toList();
        return Uni.createFrom().item(names);
    }

    /** Expose the service store for test assertions. */
    public ConcurrentHashMap<String, ServiceRecord> getServices() {
        return services;
    }

    /** Check if a service was created with the given name in any namespace. */
    public boolean hasService(String serviceName) {
        return services.values().stream().anyMatch(r -> r.serviceName().equals(serviceName));
    }

    /** Clear all recorded services (for test isolation if needed). */
    public void clear() {
        services.clear();
    }

    private static String key(String namespace, String serviceName) {
        return namespace + "/" + serviceName;
    }
}
