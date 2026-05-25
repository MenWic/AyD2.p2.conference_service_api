package ayd2.p2b.conference_service_api.feature.report.dto.internal;

import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipationTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantRow {
    private UUID userId;
    private Set<ParticipationTypeEnum> participationTypes;
}
