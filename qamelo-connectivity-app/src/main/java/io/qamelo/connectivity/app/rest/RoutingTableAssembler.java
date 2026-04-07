package io.qamelo.connectivity.app.rest;

import io.qamelo.connectivity.domain.agent.AgentRepository;
import io.qamelo.connectivity.domain.agent.AgentStatus;
import io.qamelo.connectivity.domain.agent.VirtualHostRepository;
import io.qamelo.connectivity.domain.agent.VirtualHostStatus;
import io.qamelo.connectivity.domain.spi.RoutingEntry;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class RoutingTableAssembler {

    @Inject
    AgentRepository agentRepository;

    @Inject
    VirtualHostRepository virtualHostRepository;

    public Uni<List<RoutingEntry>> assembleFullTable() {
        return agentRepository.findAll()
                .filter(a -> a.getStatus() == AgentStatus.CONNECTED || a.getStatus() == AgentStatus.REGISTERED)
                .onItem().transformToUniAndConcatenate(agent ->
                        virtualHostRepository.findByAgentId(agent.getId())
                                .map(vhs -> vhs.stream()
                                        .filter(vh -> vh.getStatus() == VirtualHostStatus.ACTIVE)
                                        .map(vh -> new RoutingEntry(
                                                agent.getId(), agent.getName(), agent.getCertSubjectSan(),
                                                vh.getHostname(), vh.getTargetHost(), vh.getTargetPort(),
                                                vh.getProtocol().name(), vh.getConnectionId()))
                                        .toList())
                )
                .collect().asList()
                .map(lists -> lists.stream().flatMap(List::stream).toList());
    }
}
