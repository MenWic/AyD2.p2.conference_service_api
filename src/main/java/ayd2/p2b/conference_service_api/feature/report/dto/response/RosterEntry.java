package ayd2.p2b.conference_service_api.feature.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Roster entry for a reserved workshop participant")
public class RosterEntry {
    @Schema(description = "Official participant personal ID resolved from IAM", example = "A1234567")
    private String personalId;

    @Schema(description = "Participant full name resolved from IAM", example = "Ana Maria Lopez")
    private String fullName;

    @Schema(description = "Participant email resolved from IAM", example = "ana.lopez@example.com")
    private String email;

    @Schema(
            description = "Primary participation type for this user in the congress. Priority: SPEAKER > WORKSHOP_LEADER > GUEST_SPEAKER > PROPOSAL_AUTHOR > ENROLLED.",
            example = "SPEAKER")
    private ParticipationTypeEnum participationType;
}
