package ayd2.p2b.conference_service_api.feature.call.dto.internal;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class CallCongressSummary {
    UUID congressId;
    UUID institutionId;
    UUID createdBy;
}
