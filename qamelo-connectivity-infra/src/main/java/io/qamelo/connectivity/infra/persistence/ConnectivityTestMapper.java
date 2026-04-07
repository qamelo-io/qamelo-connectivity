package io.qamelo.connectivity.infra.persistence;

import io.qamelo.connectivity.domain.test.ConnectivityTest;
import io.qamelo.connectivity.domain.test.TestDirection;
import io.qamelo.connectivity.domain.test.TestStatus;
import io.qamelo.connectivity.domain.test.TestType;

public final class ConnectivityTestMapper {

    private ConnectivityTestMapper() {}

    public static ConnectivityTest toDomain(ConnectivityTestEntity entity) {
        return new ConnectivityTest(
                entity.id,
                entity.connectionId,
                TestDirection.valueOf(entity.direction),
                TestType.valueOf(entity.type),
                TestStatus.valueOf(entity.status),
                entity.resultMessage,
                entity.latencyMs,
                entity.errorDetail,
                entity.startedAt,
                entity.completedAt,
                entity.initiatedBy
        );
    }

    public static ConnectivityTestEntity toEntity(ConnectivityTest test) {
        ConnectivityTestEntity entity = new ConnectivityTestEntity();
        entity.id = test.getId();
        entity.connectionId = test.getConnectionId();
        entity.direction = test.getDirection().name();
        entity.type = test.getType().name();
        entity.status = test.getStatus().name();
        entity.resultMessage = test.getResultMessage();
        entity.latencyMs = test.getLatencyMs();
        entity.errorDetail = test.getErrorDetail();
        entity.startedAt = test.getStartedAt();
        entity.completedAt = test.getCompletedAt();
        entity.initiatedBy = test.getInitiatedBy();
        return entity;
    }
}
