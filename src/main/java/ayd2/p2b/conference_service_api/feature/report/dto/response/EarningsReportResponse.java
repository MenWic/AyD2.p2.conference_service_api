package ayd2.p2b.conference_service_api.feature.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SystemAdmin financial report payload grouped by institution")
public class EarningsReportResponse {
    @Schema(description = "Institution report items", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<EarningsInstitutionItem> items;

    @Schema(description = "Total number of returned institution items", example = "2")
    private long totalItems;

    @Schema(description = "Grand total collected amount", example = "3500.00")
    private BigDecimal grandTotalAmount;

    @Schema(description = "Grand total commission amount", example = "350.00")
    private BigDecimal grandTotalCommission;

    @Schema(description = "Grand total net amount", example = "3150.00")
    private BigDecimal grandTotalNet;
}
