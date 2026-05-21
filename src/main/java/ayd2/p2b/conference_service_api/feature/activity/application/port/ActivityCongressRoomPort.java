package ayd2.p2b.conference_service_api.feature.activity.application.port;

import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivityCongressRoomSummary;

import java.util.Optional;
import java.util.UUID;

public interface ActivityCongressRoomPort {
    Optional<ActivityCongressRoomSummary> findManageableCongressById(UUID congressId);

    Optional<ActivityCongressRoomSummary> findManageableRoomById(UUID roomId);

    boolean existsPublicCongressById(UUID congressId);
}
