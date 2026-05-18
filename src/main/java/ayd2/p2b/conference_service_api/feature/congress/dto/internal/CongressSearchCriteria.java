package ayd2.p2b.conference_service_api.feature.congress.dto.internal;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
public class CongressSearchCriteria {
    UUID institutionId;
    LocalDate startDateFrom;
    LocalDate startDateTo;
    String search;
}
