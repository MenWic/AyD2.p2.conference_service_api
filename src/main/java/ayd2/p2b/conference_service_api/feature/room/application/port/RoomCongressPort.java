package ayd2.p2b.conference_service_api.feature.room.application.port;

import ayd2.p2b.conference_service_api.feature.room.dto.internal.RoomCongressSummary;

import java.util.Optional;
import java.util.UUID;

public interface RoomCongressPort {
    Optional<RoomCongressSummary> findManageableCongressById(UUID congressId);

    boolean existsPublicCongressById(UUID congressId);
}
