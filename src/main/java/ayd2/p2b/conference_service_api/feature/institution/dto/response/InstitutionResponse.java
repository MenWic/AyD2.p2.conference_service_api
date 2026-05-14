package ayd2.p2b.conference_service_api.feature.institution.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "InstitutionResponse", description = "Institution API response payload")
public class InstitutionResponse {
    @Schema(example = "c9e3e2d8-b3fb-4749-925d-02e15c8a5c07")
    private UUID id;

    @Schema(example = "Universidad de San Carlos")
    private String name;

    @Schema(example = "Public university institution")
    private String description;

    @Schema(example = "contacto@usac.edu.gt")
    private String contactEmail;

    @Schema(example = "true")
    private boolean active;

    @Schema(example = "2026-05-22T10:15:30")
    private LocalDateTime createdAt;

    @Schema(example = "2026-05-22T10:15:30")
    private LocalDateTime updatedAt;
}
