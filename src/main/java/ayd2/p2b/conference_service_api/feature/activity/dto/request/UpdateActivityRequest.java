package ayd2.p2b.conference_service_api.feature.activity.dto.request;

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
@Schema(name = "UpdateActivityRequest", description = "Request payload to update an activity")
public class UpdateActivityRequest {

    @Schema(example = "Taller de Cloud Native Avanzado")
    private String name;

    @Schema(example = "Sesion practica avanzada")
    private String description;

    @Schema(example = "f39f1f7f-e2d2-4f2e-8cb8-95630f1a9f8e")
    private UUID roomId;

    @Schema(example = "TALLER", allowableValues = {"PONENCIA", "TALLER"})
    private ActivityType type;

    @Schema(example = "2026-10-10T10:30:00Z")
    private OffsetDateTime startTime;

    @Schema(example = "2026-10-10T12:30:00Z")
    private OffsetDateTime endTime;

    @Schema(example = "40")
    private Integer workshopCapacity;

    @Schema(description = "Activity leader user IDs; when present replaces the full leader set")
    private List<UUID> leaders;
}
