package ayd2.p2b.conference_service_api.feature.room.application.update;

import ayd2.p2b.conference_service_api.feature.room.application.port.RoomCongressPort;
import ayd2.p2b.conference_service_api.feature.room.application.port.RoomRepositoryPort;
import ayd2.p2b.conference_service_api.feature.room.application.support.RoomAccessPolicy;
import ayd2.p2b.conference_service_api.feature.room.application.exception.RoomExceptions;
import ayd2.p2b.conference_service_api.feature.room.domain.model.Room;
import ayd2.p2b.conference_service_api.feature.room.dto.internal.RoomCongressSummary;
import ayd2.p2b.conference_service_api.feature.room.dto.internal.RoomRequesterContext;
import ayd2.p2b.conference_service_api.feature.room.dto.request.UpdateRoomRequest;
import ayd2.p2b.conference_service_api.feature.room.dto.response.RoomResponse;
import ayd2.p2b.conference_service_api.feature.room.mapper.RoomMapper;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static ayd2.p2b.conference_service_api.feature.room.application.RoomInputValidator.optionalPositiveCapacity;
import static ayd2.p2b.conference_service_api.feature.room.application.RoomInputValidator.optionalTrimmedNonBlank;
import static ayd2.p2b.conference_service_api.feature.room.application.RoomInputValidator.optionalTrimmedToNull;

@Component
@Transactional
public class UpdateRoomUseCase {

    private final RoomRepositoryPort roomRepositoryPort;
    private final RoomCongressPort roomCongressPort;
    private final IamUserLookupPort iamUserLookupPort;
    private final RoomMapper roomMapper;

    public UpdateRoomUseCase(
            RoomRepositoryPort roomRepositoryPort,
            RoomCongressPort roomCongressPort,
            IamUserLookupPort iamUserLookupPort,
            RoomMapper roomMapper
    ) {
        this.roomRepositoryPort = roomRepositoryPort;
        this.roomCongressPort = roomCongressPort;
        this.iamUserLookupPort = iamUserLookupPort;
        this.roomMapper = roomMapper;
    }

    public RoomResponse execute(UUID roomId, UpdateRoomRequest request, RoomRequesterContext requester) {
        RoomAccessPolicy.ensureCongressAdminWrite(requester);

        Room current = roomRepositoryPort.findById(roomId)
                .orElseThrow(() -> RoomExceptions.roomNotFound(roomId));

        RoomCongressSummary congress = roomCongressPort.findManageableCongressById(current.getCongressId())
                .orElseThrow(() -> RoomExceptions.congressNotFound(current.getCongressId()));
        authorizeCongressAccess(requester, congress);

        String nextName = optionalTrimmedNonBlank(request.getName(), "name");
        if (nextName != null && roomRepositoryPort.existsByCongressIdAndNameAndIdNot(current.getCongressId(), nextName, roomId)) {
            throw RoomExceptions.roomNameConflict(current.getCongressId(), nextName);
        }

        Integer nextCapacity = request.getCapacity() != null
                ? optionalPositiveCapacity(request.getCapacity())
                : current.getCapacity();

        String nextLocation = request.getLocation() != null
                ? optionalTrimmedToNull(request.getLocation())
                : current.getLocation();

        Room roomToSave = current.toBuilder()
                .name(nextName != null ? nextName : current.getName())
                .capacity(nextCapacity)
                .location(nextLocation)
                .updatedBy(requester.getUserId())
                .updatedAt(LocalDateTime.now())
                .build();

        Room saved = roomRepositoryPort.save(roomToSave);
        return roomMapper.toResponse(saved);
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
