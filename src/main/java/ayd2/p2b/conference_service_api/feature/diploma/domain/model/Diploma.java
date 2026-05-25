package ayd2.p2b.conference_service_api.feature.diploma.domain.model;

import ayd2.p2b.conference_service_api.feature.diploma.domain.exception.DiplomaDomainException;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class Diploma {
    UUID id;
    UUID userId;
    UUID congressId;
    DiplomaType type;
    UUID activityId;
    OffsetDateTime issuedAt;
    UUID createdBy;
    OffsetDateTime createdAt;

    public void validateInvariants() {
        if (userId == null) {
            throw new DiplomaDomainException("userId is required");
        }
        if (congressId == null) {
            throw new DiplomaDomainException("congressId is required");
        }
        if (type == null) {
            throw new DiplomaDomainException("type is required");
        }
        if (issuedAt == null) {
            throw new DiplomaDomainException("issuedAt is required");
        }
        if (createdBy == null) {
            throw new DiplomaDomainException("createdBy is required");
        }
        if (type == DiplomaType.PARTICIPATION && activityId != null) {
            throw new DiplomaDomainException("activityId must be null for PARTICIPATION diplomas");
        }
        if (type == DiplomaType.LEADERSHIP && activityId == null) {
            throw new DiplomaDomainException("activityId is required for LEADERSHIP diplomas");
        }
    }
}
