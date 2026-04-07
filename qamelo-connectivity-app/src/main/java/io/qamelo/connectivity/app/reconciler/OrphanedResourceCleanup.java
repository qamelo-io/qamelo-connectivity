package io.qamelo.connectivity.app.reconciler;

import io.qamelo.connectivity.domain.agent.KubernetesServiceManager;
import io.qamelo.connectivity.domain.agent.VirtualHostRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class OrphanedResourceCleanup {

    private static final Logger LOG = Logger.getLogger(OrphanedResourceCleanup.class);

    @Inject
    KubernetesServiceManager kubernetesServiceManager;

    @Inject
    VirtualHostRepository virtualHostRepository;

    @ConfigProperty(name = "qamelo.k8s.runtime-namespace", defaultValue = "qamelo-runtime")
    String runtimeNamespace;

    @io.quarkus.scheduler.Scheduled(every = "1h")
    Uni<Void> cleanupOrphanedServices() {
        return virtualHostRepository.findAll()
                .map(vh -> vh.getHostname())
                .collect().asList()
                .chain(dbHostnames -> {
                    Set<String> dbSet = dbHostnames.stream().collect(Collectors.toSet());
                    return kubernetesServiceManager.listManagedServiceNames(runtimeNamespace)
                            .chain(k8sServiceNames -> {
                                if (k8sServiceNames.isEmpty()) {
                                    return Uni.createFrom().voidItem();
                                }
                                return Multi.createFrom().iterable(k8sServiceNames)
                                        .filter(name -> !dbSet.contains(name))
                                        .onItem().transformToUniAndConcatenate(orphan -> {
                                            LOG.infof("Deleting orphaned K8s service %s/%s", runtimeNamespace, orphan);
                                            return kubernetesServiceManager.deleteService(runtimeNamespace, orphan)
                                                    .onFailure().recoverWithUni(ex -> {
                                                        LOG.warnf(ex, "Failed to delete orphaned service %s", orphan);
                                                        return Uni.createFrom().voidItem();
                                                    });
                                        })
                                        .collect().last()
                                        .replaceWithVoid();
                            });
                });
    }
}
