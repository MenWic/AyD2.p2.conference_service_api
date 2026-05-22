package ayd2.p2b.conference_service_api.feature.committee.dto.response;

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
@Schema(name = "CommitteeMemberResponse", description = "Scientific committee member API response payload")
public class CommitteeMemberResponse {
    @Schema(example = "9470bdea-8e37-4d0e-b2ae-545211ec4498")
    private UUID congressId;

    @Schema(example = "18d7f0d1-20cf-47b0-a3eb-d8a292e3f3d8")
    private UUID userId;

    @Schema(example = "Ana Maria Lopez")
    private String fullName;

    @Schema(example = "ana.lopez@example.com")
    private String email;

    @Schema(example = "2026-10-10T10:00:00Z")
    private OffsetDateTime addedAt;
}
