package ayd2.p2b.conference_service_api.feature.room.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class Room {
    UUID id;
    UUID congressId;
    String name;
    Integer capacity;
    String location;
    UUID createdBy;
    LocalDateTime createdAt;
    UUID updatedBy;
    LocalDateTime updatedAt;
}
