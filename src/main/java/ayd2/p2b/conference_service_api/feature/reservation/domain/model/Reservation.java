package ayd2.p2b.conference_service_api.feature.reservation.domain.model;

import ayd2.p2b.conference_service_api.feature.reservation.domain.exception.ReservationDomainException;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class Reservation {
    UUID id;
    UUID activityId;
    UUID userId;
    OffsetDateTime reservedAt;
    UUID createdBy;

    public void validateInvariants() {
        if (activityId == null) {
            throw new ReservationDomainException("activityId is required");
        }
        if (userId == null) {
            throw new ReservationDomainException("userId is required");
        }
        if (createdBy == null) {
            throw new ReservationDomainException("createdBy is required");
        }
    }
}
