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
public class DiplomaPrintDataResponse {
    private UUID diplomaId;
    private UUID userId;
    private String userFullName;
    private UUID congressId;
    private String congressName;
    private UUID activityId;
    private String activityName;
    private DiplomaType type;
    private OffsetDateTime issuedAt;
}
