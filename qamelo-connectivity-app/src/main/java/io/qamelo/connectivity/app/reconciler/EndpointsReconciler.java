package io.qamelo.connectivity.app.reconciler;

import io.qamelo.connectivity.domain.agent.KubernetesServiceManager;
import io.qamelo.connectivity.domain.agent.VirtualHostRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class EndpointsReconciler {

    private static final Logger LOG = Logger.getLogger(EndpointsReconciler.class);

    @Inject
    KubernetesServiceManager kubernetesServiceManager;

    @Inject
    VirtualHostRepository virtualHostRepository;

    @ConfigProperty(name = "qamelo.k8s.runtime-namespace", defaultValue = "qamelo-runtime")
    String runtimeNamespace;

    @ConfigProperty(name = "qamelo.k8s.gateway-ip", defaultValue = "10.96.0.100")
    String gatewayIp;

    @io.quarkus.scheduler.Scheduled(every = "30s")
    Uni<Void> reconcileEndpoints() {
        return kubernetesServiceManager.listManagedServiceNames(runtimeNamespace)
                .chain(serviceNames -> {
                    if (serviceNames.isEmpty()) {
                        return Uni.createFrom().voidItem();
                    }
                    return Multi.createFrom().iterable(serviceNames)
                            .onItem().transformToUniAndConcatenate(serviceName ->
                                    kubernetesServiceManager.updateEndpoints(
                                            runtimeNamespace, serviceName,
                                            List.of(gatewayIp), 0) // port will be from existing endpoints
                                            .onFailure().recoverWithUni(ex -> {
                                                LOG.warnf(ex, "Failed to reconcile endpoints for service %s", serviceName);
                                                return Uni.createFrom().voidItem();
                                            })
                            )
                            .collect().last()
                            .replaceWithVoid();
                });
    }
}
