package ayd2.p2b.conference_service_api.feature.diploma.dto.internal;

import ayd2.p2b.conference_service_api.feature.diploma.domain.model.DiplomaType;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class DiplomaEligibilityCandidate {
    UUID userId;
    UUID congressId;
    String congressName;
    DiplomaType type;
    UUID activityId;
    String activityName;
}
