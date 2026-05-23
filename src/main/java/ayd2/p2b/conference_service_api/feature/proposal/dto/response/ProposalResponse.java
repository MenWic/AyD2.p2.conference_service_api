package ayd2.p2b.conference_service_api.feature.proposal.dto.response;

import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalStatus;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "ProposalResponse", description = "Proposal API response payload")
public class ProposalResponse {
    @Schema(example = "f9b5c6f2-88d4-4ef2-a3cf-2728d1f7fe7f")
    private UUID id;

    @Schema(example = "7a5fd79c-b35f-4df3-b4e4-7f017a83f96a")
    private UUID callId;

    @Schema(example = "18d7f0d1-20cf-47b0-a3eb-d8a292e3f3d8")
    private UUID authorUserId;

    @Schema(example = "Arquitectura basada en eventos para sistemas de conferencias")
    private String title;

    @Schema(example = "Trabajo que describe un enfoque de microservicios y eventos para alta concurrencia.")
    private String description;

    @Schema(example = "PONENCIA", allowableValues = {"PONENCIA", "TALLER"})
    private ProposalType type;

    @Schema(example = "PENDING", allowableValues = {"PENDING", "APPROVED", "REJECTED"})
    private ProposalStatus status;

    @Schema(example = "18d7f0d1-20cf-47b0-a3eb-d8a292e3f3d8")
    private UUID reviewedBy;

    @Schema(example = "2026-10-10T10:00:00Z")
    private OffsetDateTime reviewedAt;

    @Schema(example = "2026-10-09T15:10:00Z")
    private OffsetDateTime createdAt;
}
