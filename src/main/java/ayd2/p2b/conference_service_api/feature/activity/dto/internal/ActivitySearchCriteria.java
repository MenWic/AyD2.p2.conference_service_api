package ayd2.p2b.conference_service_api.feature.activity.dto.internal;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class ActivitySearchCriteria {
    UUID roomId;
    ActivityType type;
    OffsetDateTime dateFrom;
    OffsetDateTime dateTo;
}
