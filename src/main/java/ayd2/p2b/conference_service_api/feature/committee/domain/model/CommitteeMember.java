package ayd2.p2b.conference_service_api.feature.committee.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class CommitteeMember {
    UUID congressId;
    UUID userId;
    OffsetDateTime addedAt;
    UUID addedBy;
}
