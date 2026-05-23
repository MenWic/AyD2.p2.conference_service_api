package ayd2.p2b.conference_service_api.feature.proposal.dto.request;

import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateProposalRequest", description = "Request payload to submit a proposal to an open call")
public class CreateProposalRequest {

    @NotBlank(message = "title is required")
    @Size(max = 500, message = "title must not exceed 500 characters")
    @Schema(example = "Arquitectura basada en eventos para sistemas de conferencias")
    private String title;

    @NotBlank(message = "description is required")
    @Schema(example = "Trabajo que describe un enfoque de microservicios y eventos para alta concurrencia.")
    private String description;

    @NotNull(message = "type is required")
    @Schema(example = "PONENCIA", allowableValues = {"PONENCIA", "TALLER"})
    private ProposalType type;
}
