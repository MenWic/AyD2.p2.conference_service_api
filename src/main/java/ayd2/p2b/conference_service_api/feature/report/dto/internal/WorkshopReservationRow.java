package ayd2.p2b.conference_service_api.feature.report.dto.internal;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class WorkshopReservationRow {
    UUID activityId;
    String activityName;
    int workshopCapacity;
    List<UUID> reservedUserIds;
}
