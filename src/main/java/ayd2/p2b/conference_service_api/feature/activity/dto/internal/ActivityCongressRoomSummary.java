package ayd2.p2b.conference_service_api.feature.activity.dto.internal;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class ActivityCongressRoomSummary {
    UUID congressId;
    UUID institutionId;
    UUID roomId;
    UUID createdBy;
}
