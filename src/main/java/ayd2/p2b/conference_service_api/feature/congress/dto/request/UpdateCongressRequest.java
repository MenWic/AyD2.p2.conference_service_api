package ayd2.p2b.conference_service_api.feature.congress.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
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
@Schema(name = "UpdateCongressRequest", description = "Request payload to update a congress")
public class UpdateCongressRequest {

    @Schema(example = "Congreso Nacional de Ingenieria 2026")
    private String name;

    @Schema(example = "Actualizacion del programa academico y de investigacion")
    private String description;

    @Schema(example = "2026-09-10")
    private LocalDate startDate;

    @Schema(example = "2026-09-12")
    private LocalDate endDate;

    @Schema(example = "Antigua Guatemala")
    private String location;

    @DecimalMin(value = "35.00", inclusive = true, message = "price must be >= 35.00")
    @Schema(example = "95.00")
    private BigDecimal price;

    @Schema(example = "d2719de1-0409-4d2e-bf9b-a06f0ea74df7")
    private UUID institutionId;
}
