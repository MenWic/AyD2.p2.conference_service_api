package ayd2.p2b.conference_service_api.feature.activity.application.schedule;

import ayd2.p2b.conference_service_api.feature.activity.domain.exception.ActivityDomainException;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityRepositoryPort;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class ActivitySchedulePolicy {

    private ActivitySchedulePolicy() {
    }

    public static void ensureNoRoomOverlap(
            ActivityRepositoryPort repositoryPort,
            UUID roomId,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            UUID excludeActivityId
    ) {
        boolean overlap = repositoryPort.existsRoomTimeOverlap(roomId, startTime, endTime, excludeActivityId);
        if (overlap) {
            throw new ActivityDomainException("Room schedule overlap detected for roomId=" + roomId);
        }
    }
}
