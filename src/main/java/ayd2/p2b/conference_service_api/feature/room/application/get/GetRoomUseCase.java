package ayd2.p2b.conference_service_api.feature.room.application.get;

import ayd2.p2b.conference_service_api.feature.room.application.exception.RoomExceptions;
import ayd2.p2b.conference_service_api.feature.room.application.port.RoomRepositoryPort;
import ayd2.p2b.conference_service_api.feature.room.dto.response.RoomResponse;
import ayd2.p2b.conference_service_api.feature.room.mapper.RoomMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class GetRoomUseCase {

    private final RoomRepositoryPort roomRepositoryPort;
    private final RoomMapper roomMapper;

    public GetRoomUseCase(RoomRepositoryPort roomRepositoryPort, RoomMapper roomMapper) {
        this.roomRepositoryPort = roomRepositoryPort;
        this.roomMapper = roomMapper;
    }

    public RoomResponse execute(UUID roomId) {
        return roomRepositoryPort.findPublicById(roomId)
                .map(roomMapper::toResponse)
                .orElseThrow(() -> RoomExceptions.roomNotFound(roomId));
    }
}
