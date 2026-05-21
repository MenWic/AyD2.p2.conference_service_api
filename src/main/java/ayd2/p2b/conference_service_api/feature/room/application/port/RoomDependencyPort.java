package ayd2.p2b.conference_service_api.feature.room.application.port;

import java.util.UUID;

public interface RoomDependencyPort {
    boolean existsActivitiesForRoom(UUID roomId);
}
