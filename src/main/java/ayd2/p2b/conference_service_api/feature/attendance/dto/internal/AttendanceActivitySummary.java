package ayd2.p2b.conference_service_api.feature.attendance.dto.internal;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class AttendanceActivitySummary {
    UUID activityId;
    UUID congressId;
    UUID institutionId;
    UUID congressCreatedBy;
    UUID roomId;
    ActivityType type;
}
