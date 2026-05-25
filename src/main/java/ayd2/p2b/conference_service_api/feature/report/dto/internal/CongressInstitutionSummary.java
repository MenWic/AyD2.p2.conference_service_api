package ayd2.p2b.conference_service_api.feature.report.dto.internal;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class CongressInstitutionSummary {
    UUID congressId;
    UUID institutionId;
    String congressName;
}
