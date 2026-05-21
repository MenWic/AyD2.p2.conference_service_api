package ayd2.p2b.conference_service_api.feature.room.application.delete;

import ayd2.p2b.conference_service_api.feature.room.application.port.RoomCongressPort;
import ayd2.p2b.conference_service_api.feature.room.application.port.RoomDependencyPort;
import ayd2.p2b.conference_service_api.feature.room.application.port.RoomRepositoryPort;
import ayd2.p2b.conference_service_api.feature.room.application.support.RoomAccessPolicy;
import ayd2.p2b.conference_service_api.feature.room.application.exception.RoomExceptions;
import ayd2.p2b.conference_service_api.feature.room.domain.model.Room;
import ayd2.p2b.conference_service_api.feature.room.dto.internal.RoomCongressSummary;
import ayd2.p2b.conference_service_api.feature.room.dto.internal.RoomRequesterContext;
import ayd2.p2b.conference_service_api.feature.room.dto.response.RoomResponse;
import ayd2.p2b.conference_service_api.feature.room.mapper.RoomMapper;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Transactional
public class DeleteRoomUseCase {

    private final RoomRepositoryPort roomRepositoryPort;
    private final RoomCongressPort roomCongressPort;
    private final RoomDependencyPort roomDependencyPort;
    private final IamUserLookupPort iamUserLookupPort;
    private final RoomMapper roomMapper;

    public DeleteRoomUseCase(
            RoomRepositoryPort roomRepositoryPort,
            RoomCongressPort roomCongressPort,
            RoomDependencyPort roomDependencyPort,
            IamUserLookupPort iamUserLookupPort,
            RoomMapper roomMapper
    ) {
        this.roomRepositoryPort = roomRepositoryPort;
        this.roomCongressPort = roomCongressPort;
        this.roomDependencyPort = roomDependencyPort;
        this.iamUserLookupPort = iamUserLookupPort;
        this.roomMapper = roomMapper;
    }

    public RoomResponse execute(UUID roomId, RoomRequesterContext requester) {
        RoomAccessPolicy.ensureCongressAdminWrite(requester);

        Room current = roomRepositoryPort.findById(roomId)
                .orElseThrow(() -> RoomExceptions.roomNotFound(roomId));

        RoomCongressSummary congress = roomCongressPort.findManageableCongressById(current.getCongressId())
                .orElseThrow(() -> RoomExceptions.congressNotFound(current.getCongressId()));
        authorizeCongressAccess(requester, congress);

        if (roomDependencyPort.existsActivitiesForRoom(roomId)) {
            throw RoomExceptions.hasActivities(roomId);
        }

        roomRepositoryPort.deleteById(roomId);

        Room deleted = current.toBuilder()
                .updatedBy(requester.getUserId())
                .updatedAt(LocalDateTime.now())
                .build();
        return roomMapper.toResponse(deleted);
    }

    private void authorizeCongressAccess(RoomRequesterContext requester, RoomCongressSummary congress) {
        if (requester.getUserId().equals(congress.getCreatedBy())) {
            RoomAccessPolicy.ensureCanManageCongress(requester, congress, false);
            return;
        }
        boolean linkedToInstitution = iamUserLookupPort.isCongressAdminLinkedToInstitution(
                requester.getUserId(),
                congress.getInstitutionId(),
                requester.getAccessToken()
        );
        RoomAccessPolicy.ensureCanManageCongress(requester, congress, linkedToInstitution);
    }
}
