package ayd2.p2b.conference_service_api.feature.congress.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateCongressRequest", description = "Request payload to create a congress")
public class CreateCongressRequest {

    @NotBlank(message = "name is required")
    @Schema(example = "Congreso Nacional de Ingenieria")
    private String name;

    @NotBlank(message = "description is required")
    @Schema(example = "Evento academico anual de investigacion y desarrollo")
    private String description;

    @NotNull(message = "startDate is required")
    @Schema(example = "2026-09-10")
    private LocalDate startDate;

    @NotNull(message = "endDate is required")
    @Schema(example = "2026-09-12")
    private LocalDate endDate;

    @NotBlank(message = "location is required")
    @Schema(example = "Ciudad de Guatemala")
    private String location;

    @NotNull(message = "price is required")
    @DecimalMin(value = "35.00", inclusive = true, message = "price must be >= 35.00")
    @Schema(example = "85.00")
    private BigDecimal price;

    @NotNull(message = "institutionId is required")
    @Schema(example = "d2719de1-0409-4d2e-bf9b-a06f0ea74df7")
    private UUID institutionId;
}
