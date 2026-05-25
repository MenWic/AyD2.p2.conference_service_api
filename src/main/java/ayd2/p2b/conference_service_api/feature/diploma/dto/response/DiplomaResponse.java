package ayd2.p2b.conference_service_api.feature.diploma.dto.response;

import ayd2.p2b.conference_service_api.feature.diploma.domain.model.DiplomaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiplomaResponse {
    private UUID id;
    private UUID userId;
    private UUID congressId;
    private DiplomaType type;
    private UUID activityId;
    private OffsetDateTime issuedAt;
    private String congressName;
    private String activityName;
    private boolean available;
}
