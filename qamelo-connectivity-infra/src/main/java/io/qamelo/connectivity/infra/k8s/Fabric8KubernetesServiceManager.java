package io.qamelo.connectivity.infra.k8s;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointAddressBuilder;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointPortBuilder;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.EndpointSubsetBuilder;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.qamelo.connectivity.domain.agent.KubernetesServiceManager;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class Fabric8KubernetesServiceManager implements KubernetesServiceManager {

    private static final Logger LOG = Logger.getLogger(Fabric8KubernetesServiceManager.class);
    private static final String MANAGED_BY_LABEL = "app.kubernetes.io/managed-by";
    private static final String MANAGED_BY_VALUE = "qamelo-connectivity";
    private static final String AGENT_ID_LABEL = "qamelo.io/agent-id";
    private static final String VIRTUAL_HOST_ID_LABEL = "qamelo.io/virtual-host-id";

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public Uni<Void> createService(String namespace, String serviceName, UUID agentId,
                                   UUID virtualHostId, String gatewayIp, int targetPort) {
        return Uni.createFrom().item(() -> {
            Map<String, String> labels = Map.of(
                    MANAGED_BY_LABEL, MANAGED_BY_VALUE,
                    AGENT_ID_LABEL, agentId.toString(),
                    VIRTUAL_HOST_ID_LABEL, virtualHostId.toString()
            );

            Service service = new ServiceBuilder()
                    .withNewMetadata()
                        .withName(serviceName)
                        .withNamespace(namespace)
                        .withLabels(labels)
                    .endMetadata()
                    .withNewSpec()
                        .withClusterIP("None")
                        .addNewPort()
                            .withPort(targetPort)
                            .withProtocol("TCP")
                        .endPort()
                    .endSpec()
                    .build();

            kubernetesClient.services().inNamespace(namespace).resource(service).create();
            LOG.infof("Created K8s Service %s/%s for agent %s, virtual host %s",
                    namespace, serviceName, agentId, virtualHostId);

            Endpoints endpoints = new EndpointsBuilder()
                    .withNewMetadata()
                        .withName(serviceName)
                        .withNamespace(namespace)
                        .withLabels(labels)
                    .endMetadata()
                    .withSubsets(buildSubsets(List.of(gatewayIp), targetPort))
                    .build();

            kubernetesClient.endpoints().inNamespace(namespace).resource(endpoints).create();
            LOG.infof("Created K8s Endpoints %s/%s -> %s:%d", namespace, serviceName, gatewayIp, targetPort);

            return null;
        }).replaceWithVoid();
    }

    @Override
    public Uni<Void> updateEndpoints(String namespace, String serviceName,
                                     List<String> gatewayIps, int targetPort) {
        return Uni.createFrom().item(() -> {
            Endpoints existing = kubernetesClient.endpoints().inNamespace(namespace)
                    .withName(serviceName).get();

            if (existing == null) {
                LOG.warnf("Endpoints %s/%s not found, skipping update", namespace, serviceName);
                return null;
            }

            existing.setSubsets(buildSubsets(gatewayIps, targetPort));
            kubernetesClient.endpoints().inNamespace(namespace).resource(existing).update();
            LOG.infof("Updated K8s Endpoints %s/%s with %d addresses", namespace, serviceName, gatewayIps.size());

            return null;
        }).replaceWithVoid();
    }

    @Override
    public Uni<Void> deleteService(String namespace, String serviceName) {
        return Uni.createFrom().item(() -> {
            kubernetesClient.services().inNamespace(namespace).withName(serviceName).delete();
            kubernetesClient.endpoints().inNamespace(namespace).withName(serviceName).delete();
            LOG.infof("Deleted K8s Service + Endpoints %s/%s", namespace, serviceName);
            return null;
        }).replaceWithVoid();
    }

    @Override
    public Uni<List<String>> listManagedServiceNames(String namespace) {
        return Uni.createFrom().item(() -> {
            ServiceList serviceList = kubernetesClient.services().inNamespace(namespace)
                    .withLabel(MANAGED_BY_LABEL, MANAGED_BY_VALUE)
                    .list();
            return serviceList.getItems().stream()
                    .map(svc -> svc.getMetadata().getName())
                    .toList();
        });
    }

    private List<EndpointSubset> buildSubsets(List<String> ips, int port) {
        List<EndpointAddress> addresses = ips.stream()
                .map(ip -> new EndpointAddressBuilder().withIp(ip).build())
                .toList();
        EndpointPort endpointPort = new EndpointPortBuilder()
                .withPort(port)
                .withProtocol("TCP")
                .build();
        return List.of(new EndpointSubsetBuilder()
                .withAddresses(addresses)
                .withPorts(endpointPort)
                .build());
    }
}
