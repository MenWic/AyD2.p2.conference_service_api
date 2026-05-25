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
@Schema(description = "CongressAdmin financial report payload grouped by congress")
public class EarningsByCongressReportResponse {
    @Schema(description = "Report items", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<EarningsByCongressItem> items;

    @Schema(description = "Total number of returned items", example = "1")
    private long totalItems;

    @Schema(description = "Grand total collected amount", example = "1200.00")
    private BigDecimal grandTotalAmount;

    @Schema(description = "Grand total commission amount", example = "120.00")
    private BigDecimal grandTotalCommission;

    @Schema(description = "Grand total net amount", example = "1080.00")
    private BigDecimal grandTotalNet;
}
