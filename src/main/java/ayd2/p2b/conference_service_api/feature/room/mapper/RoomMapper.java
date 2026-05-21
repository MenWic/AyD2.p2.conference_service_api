package ayd2.p2b.conference_service_api.feature.room.mapper;

import ayd2.p2b.conference_service_api.feature.room.domain.model.Room;
import ayd2.p2b.conference_service_api.feature.room.dto.response.RoomResponse;
import ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.entity.RoomEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    Room toDomain(RoomEntity entity);

    @Mapping(target = "congress", ignore = true)
    RoomEntity toEntity(Room room);

    RoomResponse toResponse(Room room);
}
