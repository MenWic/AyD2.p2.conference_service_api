package ayd2.p2b.conference_service_api.feature.attendance.domain.model;

import ayd2.p2b.conference_service_api.feature.attendance.domain.exception.AttendanceDomainException;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class Attendance {
    UUID id;
    UUID activityId;
    UUID userId;
    String personalIdSnapshot;
    UUID registeredBy;
    OffsetDateTime registeredAt;
    UUID createdBy;
    OffsetDateTime createdAt;

    public void validateInvariants() {
        if (activityId == null) {
            throw new AttendanceDomainException("activityId is required");
        }
        if (userId == null) {
            throw new AttendanceDomainException("userId is required");
        }
        if (personalIdSnapshot == null || personalIdSnapshot.trim().isBlank()) {
            throw new AttendanceDomainException("personalIdSnapshot is required");
        }
        if (registeredBy == null) {
            throw new AttendanceDomainException("registeredBy is required");
        }
        if (createdBy == null) {
            throw new AttendanceDomainException("createdBy is required");
        }
    }
}
