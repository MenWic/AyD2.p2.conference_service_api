package ayd2.p2b.conference_service_api.feature.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "One congress row in the SystemAdmin congresses-by-institution report")
public class CongressByInstitutionItem {
    @Schema(description = "Institution identifier", example = "d2719de1-0409-4d2e-bf9b-a06f0ea74df7")
    private UUID institutionId;

    @Schema(description = "Institution name", example = "Universidad de San Carlos")
    private String institutionName;

    @Schema(description = "Congress identifier", example = "7d899e63-481d-4df8-87f1-7a8d8f437b68")
    private UUID congressId;

    @Schema(description = "Congress name", example = "Congreso Nacional de Ingenieria")
    private String congressName;

    @Schema(description = "Congress start date", example = "2026-09-10")
    private LocalDate startDate;

    @Schema(description = "Congress end date", example = "2026-09-12")
    private LocalDate endDate;

    @Schema(description = "Congress location", example = "Ciudad de Guatemala")
    private String location;

    @Schema(description = "Congress price", example = "85.00")
    private BigDecimal price;
}
