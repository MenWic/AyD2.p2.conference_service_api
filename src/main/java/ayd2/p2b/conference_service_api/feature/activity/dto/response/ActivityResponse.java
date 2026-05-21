package ayd2.p2b.conference_service_api.feature.activity.dto.response;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ActivityResponse", description = "Activity API response payload")
public class ActivityResponse {
    @Schema(example = "3a6d8f8e-a077-4a66-a7d3-c8dd90666fc2")
    private UUID id;

    @Schema(example = "9470bdea-8e37-4d0e-b2ae-545211ec4498")
    private UUID congressId;

    @Schema(example = "f39f1f7f-e2d2-4f2e-8cb8-95630f1a9f8e")
    private UUID roomId;

    @Schema(example = "Taller de Cloud Native")
    private String name;

    @Schema(example = "Sesion practica para despliegues en Kubernetes")
    private String description;

    @Schema(example = "TALLER", allowableValues = {"PONENCIA", "TALLER"})
    private ActivityType type;

    @Schema(example = "2026-10-10T10:00:00Z")
    private OffsetDateTime startTime;

    @Schema(example = "2026-10-10T12:00:00Z")
    private OffsetDateTime endTime;

    @Schema(example = "[\"d0952fd5-4eb8-467b-93a2-3ee586d0e20b\"]")
    private List<UUID> leaders;

    @Schema(example = "30")
    private Integer workshopCapacity;

    @Schema(example = "2026-05-22T10:15:30Z")
    private OffsetDateTime createdAt;

    @Schema(example = "2026-05-22T11:30:45Z")
    private OffsetDateTime updatedAt;
}
