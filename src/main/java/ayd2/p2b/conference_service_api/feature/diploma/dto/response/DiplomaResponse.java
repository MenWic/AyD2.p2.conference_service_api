package ayd2.p2b.conference_service_api.feature.diploma.dto.response;

import ayd2.p2b.conference_service_api.feature.diploma.domain.model.DiplomaType;
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
@Schema(name = "DiplomaResponse", description = "Diploma metadata visible to the diploma owner")
public class DiplomaResponse {
    @Schema(description = "Diploma identifier (UUID)", example = "d65a773a-d57e-41e8-8782-02f2f5aaf0a2")
    private UUID id;
    @Schema(description = "Diploma owner user identifier (UUID)", example = "18d7f0d1-20cf-47b0-a3eb-d8a292e3f3d8")
    private UUID userId;
    @Schema(description = "Congress identifier associated to the diploma (UUID)", example = "9470bdea-8e37-4d0e-b2ae-545211ec4498")
    private UUID congressId;
    @Schema(description = "Diploma type", example = "PARTICIPATION", allowableValues = {"PARTICIPATION", "LEADERSHIP"})
    private DiplomaType type;
    @Schema(description = "Activity identifier (UUID). Null for PARTICIPATION diplomas", nullable = true, example = "3a6d8f8e-a077-4a66-a7d3-c8dd90666fc2")
    private UUID activityId;
    @Schema(description = "Diploma materialization timestamp (ISO-8601 date-time)", example = "2026-10-10T10:00:00Z")
    private OffsetDateTime issuedAt;
    @Schema(description = "Congress display name", example = "Congreso Nacional de Ingenieria")
    private String congressName;
    @Schema(description = "Activity display name. Null for PARTICIPATION diplomas", nullable = true, example = "Taller de Cloud Native")
    private String activityName;
    @Schema(description = "Availability flag. Materialized diplomas are returned as true", example = "true")
    private boolean available;
}
