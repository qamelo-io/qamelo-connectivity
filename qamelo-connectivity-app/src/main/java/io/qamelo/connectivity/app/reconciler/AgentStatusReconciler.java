package io.qamelo.connectivity.app.reconciler;

import io.qamelo.connectivity.domain.agent.AgentRepository;
import io.qamelo.connectivity.domain.agent.AgentStatus;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;

@ApplicationScoped
public class AgentStatusReconciler {

    private static final Logger LOG = Logger.getLogger(AgentStatusReconciler.class);
    private static final Duration STALE_THRESHOLD = Duration.ofMinutes(5);

    @Inject
    AgentRepository agentRepository;

    @io.quarkus.scheduler.Scheduled(every = "30s")
    Uni<Void> reconcileStaleAgents() {
        return agentRepository.findAll()
                .filter(agent -> agent.getStatus() == AgentStatus.CONNECTED
                        && agent.getLastSeenAt() != null
                        && agent.getLastSeenAt().plus(STALE_THRESHOLD).isBefore(Instant.now()))
                .onItem().transformToUniAndConcatenate(agent -> {
                    LOG.infof("Marking agent %s (%s) as DISCONNECTED — last seen at %s",
                            agent.getId(), agent.getName(), agent.getLastSeenAt());
                    agent.setStatus(AgentStatus.DISCONNECTED);
                    agent.setModifiedAt(Instant.now());
                    return agentRepository.update(agent).replaceWithVoid();
                })
                .collect().last()
                .replaceWithVoid();
    }
}
