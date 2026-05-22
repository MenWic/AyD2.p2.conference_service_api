package ayd2.p2b.conference_service_api.feature.committee.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AddCommitteeMemberRequest", description = "Request payload to add a committee member")
public class AddCommitteeMemberRequest {

    @NotNull(message = "userId is required")
    @Schema(example = "18d7f0d1-20cf-47b0-a3eb-d8a292e3f3d8")
    private UUID userId;
}
