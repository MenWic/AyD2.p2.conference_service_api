package ayd2.p2b.conference_service_api.feature.room.application.create;

import ayd2.p2b.conference_service_api.feature.room.application.port.RoomCongressPort;
import ayd2.p2b.conference_service_api.feature.room.application.port.RoomRepositoryPort;
import ayd2.p2b.conference_service_api.feature.room.application.support.RoomAccessPolicy;
import ayd2.p2b.conference_service_api.feature.room.application.exception.RoomExceptions;
import ayd2.p2b.conference_service_api.feature.room.domain.model.Room;
import ayd2.p2b.conference_service_api.feature.room.dto.internal.RoomCongressSummary;
import ayd2.p2b.conference_service_api.feature.room.dto.internal.RoomRequesterContext;
import ayd2.p2b.conference_service_api.feature.room.dto.request.CreateRoomRequest;
import ayd2.p2b.conference_service_api.feature.room.dto.response.RoomResponse;
import ayd2.p2b.conference_service_api.feature.room.mapper.RoomMapper;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static ayd2.p2b.conference_service_api.feature.room.application.RoomInputValidator.optionalPositiveCapacity;
import static ayd2.p2b.conference_service_api.feature.room.application.RoomInputValidator.optionalTrimmedToNull;
import static ayd2.p2b.conference_service_api.feature.room.application.RoomInputValidator.requiredTrimmed;

@Component
@Transactional
public class CreateRoomUseCase {

    private final RoomRepositoryPort roomRepositoryPort;
    private final RoomCongressPort roomCongressPort;
    private final IamUserLookupPort iamUserLookupPort;
    private final RoomMapper roomMapper;

    public CreateRoomUseCase(
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

    public RoomResponse execute(UUID congressId, CreateRoomRequest request, RoomRequesterContext requester) {
        RoomAccessPolicy.ensureCongressAdminWrite(requester);

        RoomCongressSummary congress = roomCongressPort.findManageableCongressById(congressId)
                .orElseThrow(() -> RoomExceptions.congressNotFound(congressId));
        authorizeCongressAccess(requester, congress);

        String name = requiredTrimmed(request.getName(), "name");
        Integer capacity = optionalPositiveCapacity(request.getCapacity());
        String location = optionalTrimmedToNull(request.getLocation());

        if (roomRepositoryPort.existsByCongressIdAndName(congressId, name)) {
            throw RoomExceptions.roomNameConflict(congressId, name);
        }

        Room roomToSave = Room.builder()
                .congressId(congressId)
                .name(name)
                .capacity(capacity)
                .location(location)
                .createdBy(requester.getUserId())
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
