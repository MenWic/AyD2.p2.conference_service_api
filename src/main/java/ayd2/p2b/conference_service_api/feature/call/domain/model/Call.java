package ayd2.p2b.conference_service_api.feature.call.domain.model;

import ayd2.p2b.conference_service_api.feature.call.domain.exception.CallDomainException;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class Call {
    UUID id;
    UUID congressId;
    CallStatus status;
    OffsetDateTime openedAt;
    OffsetDateTime closedAt;
    UUID createdBy;
    UUID updatedBy;

    public void validateInvariants() {
        if (status == null) {
            throw new CallDomainException("status is required");
        }
        if (status == CallStatus.OPEN && closedAt != null) {
            throw new CallDomainException("closedAt must be null while status is OPEN");
        }
        if (status == CallStatus.CLOSED && closedAt == null) {
            throw new CallDomainException("closedAt is required while status is CLOSED");
        }
    }

    public Call close(UUID updaterId, OffsetDateTime closedAtValue) {
        if (status == CallStatus.CLOSED) {
            throw new CallDomainException("Call is already CLOSED");
        }
        if (closedAtValue == null) {
            throw new CallDomainException("closedAt is required to close the call");
        }
        return toBuilder()
                .status(CallStatus.CLOSED)
                .closedAt(closedAtValue)
                .updatedBy(updaterId)
                .build();
    }
}
