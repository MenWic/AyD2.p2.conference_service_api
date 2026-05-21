package ayd2.p2b.conference_service_api.feature.room.application.list;

import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.room.application.exception.RoomExceptions;
import ayd2.p2b.conference_service_api.feature.room.application.port.RoomCongressPort;
import ayd2.p2b.conference_service_api.feature.room.application.port.RoomRepositoryPort;
import ayd2.p2b.conference_service_api.feature.room.dto.response.RoomResponse;
import ayd2.p2b.conference_service_api.feature.room.mapper.RoomMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class ListRoomsUseCase {

    private final RoomRepositoryPort roomRepositoryPort;
    private final RoomCongressPort roomCongressPort;
    private final RoomMapper roomMapper;

    public ListRoomsUseCase(
            RoomRepositoryPort roomRepositoryPort,
            RoomCongressPort roomCongressPort,
            RoomMapper roomMapper
    ) {
        this.roomRepositoryPort = roomRepositoryPort;
        this.roomCongressPort = roomCongressPort;
        this.roomMapper = roomMapper;
    }

    public PageResponse<RoomResponse> execute(UUID congressId, Pageable pageable) {
        if (!roomCongressPort.existsPublicCongressById(congressId)) {
            throw RoomExceptions.congressNotFound(congressId);
        }

        Page<RoomResponse> page = roomRepositoryPort.findPublicByCongressId(congressId, pageable)
                .map(roomMapper::toResponse);

        return PageResponse.<RoomResponse>builder()
                .items(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
