package ayd2.p2b.conference_service_api.feature.activity.dto.request;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(name = "CreateActivityRequest", description = "Request payload to create an activity inside a congress")
public class CreateActivityRequest {

    @NotBlank(message = "name is required")
    @Schema(example = "Taller de Cloud Native")
    private String name;

    @NotBlank(message = "description is required")
    @Schema(example = "Sesion practica para despliegues en Kubernetes")
    private String description;

    @NotNull(message = "roomId is required")
    @Schema(example = "f39f1f7f-e2d2-4f2e-8cb8-95630f1a9f8e")
    private UUID roomId;

    @NotNull(message = "type is required")
    @Schema(example = "TALLER", allowableValues = {"PONENCIA", "TALLER"})
    private ActivityType type;

    @NotNull(message = "startTime is required")
    @Schema(example = "2026-10-10T10:00:00Z")
    private OffsetDateTime startTime;

    @NotNull(message = "endTime is required")
    @Schema(example = "2026-10-10T12:00:00Z")
    private OffsetDateTime endTime;

    @Schema(example = "30")
    private Integer workshopCapacity;

    @Schema(description = "Activity leader user IDs")
    private List<UUID> leaders;
}
