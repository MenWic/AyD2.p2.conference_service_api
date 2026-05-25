package ayd2.p2b.conference_service_api.feature.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SystemAdmin report payload listing congresses grouped by institution")
public class CongressesByInstitutionReportResponse {
    @Schema(description = "Report items", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<CongressByInstitutionItem> items;

    @Schema(description = "Total number of returned items", example = "2")
    private long totalItems;
}
