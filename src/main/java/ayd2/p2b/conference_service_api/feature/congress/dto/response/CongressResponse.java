package ayd2.p2b.conference_service_api.feature.congress.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CongressResponse", description = "Congress response payload")
public class CongressResponse {
    @Schema(example = "7d899e63-481d-4df8-87f1-7a8d8f437b68")
    private UUID id;
    @Schema(example = "Congreso Nacional de Ingenieria")
    private String name;
    @Schema(example = "Evento academico anual de investigacion y desarrollo")
    private String description;
    @Schema(example = "2026-09-10")
    private LocalDate startDate;
    @Schema(example = "2026-09-12")
    private LocalDate endDate;
    @Schema(example = "Ciudad de Guatemala")
    private String location;
    @Schema(example = "85.00")
    private BigDecimal price;
    @Schema(example = "d2719de1-0409-4d2e-bf9b-a06f0ea74df7")
    private UUID institutionId;
    @Schema(example = "Universidad de San Carlos")
    private String institutionName;
    @Schema(example = "594f22de-fb77-471f-a8e4-57507dd7297a")
    private UUID createdBy;
    @Schema(example = "2026-05-22T10:15:30")
    private LocalDateTime createdAt;
    @Schema(example = "2026-05-22T10:15:30")
    private LocalDateTime updatedAt;
}
