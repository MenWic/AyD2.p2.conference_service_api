package ayd2.p2b.conference_service_api.feature.room.dto.internal;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class RoomCongressSummary {
    UUID id;
    UUID institutionId;
    UUID createdBy;
}
