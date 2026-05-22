package ayd2.p2b.conference_service_api.feature.call.dto.response;

import ayd2.p2b.conference_service_api.feature.call.domain.model.CallStatus;
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
@Schema(name = "CallResponse", description = "Call API response payload")
public class CallResponse {
    @Schema(example = "f9b5c6f2-88d4-4ef2-a3cf-2728d1f7fe7f")
    private UUID id;

    @Schema(example = "9470bdea-8e37-4d0e-b2ae-545211ec4498")
    private UUID congressId;

    @Schema(example = "OPEN", allowableValues = {"OPEN", "CLOSED"})
    private CallStatus status;

    @Schema(example = "2026-10-10T10:00:00Z")
    private OffsetDateTime openedAt;

    @Schema(example = "2026-10-15T18:00:00Z")
    private OffsetDateTime closedAt;
}
