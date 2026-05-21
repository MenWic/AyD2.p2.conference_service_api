package ayd2.p2b.conference_service_api.feature.activity.domain.model;

import ayd2.p2b.conference_service_api.feature.activity.domain.exception.ActivityDomainException;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class Activity {
    UUID id;
    UUID congressId;
    UUID roomId;
    String name;
    String description;
    ActivityType type;
    OffsetDateTime startTime;
    OffsetDateTime endTime;
    Integer workshopCapacity;
    UUID createdBy;
    OffsetDateTime createdAt;
    UUID updatedBy;
    OffsetDateTime updatedAt;

    public void validateInvariants() {
        validateTimeRange(startTime, endTime);
        validateWorkshopCapacity(type, workshopCapacity);
    }

    public static void validateTimeRange(OffsetDateTime startTime, OffsetDateTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new ActivityDomainException(
                    "startTime must be before endTime. startTime=" + startTime + ", endTime=" + endTime
            );
        }
    }

    public static void validateWorkshopCapacity(ActivityType type, Integer workshopCapacity) {
        if (type == null) {
            throw new ActivityDomainException("type is required");
        }

        if (type == ActivityType.TALLER) {
            if (workshopCapacity == null || workshopCapacity <= 0) {
                throw new ActivityDomainException("workshopCapacity must be > 0 for TALLER");
            }
            return;
        }

        if (workshopCapacity != null) {
            throw new ActivityDomainException("workshopCapacity must be null for PONENCIA");
        }
    }
}
