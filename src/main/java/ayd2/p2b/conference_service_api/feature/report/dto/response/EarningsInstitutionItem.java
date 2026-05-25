package ayd2.p2b.conference_service_api.feature.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Institution-level financial aggregation with nested congress items")
public class EarningsInstitutionItem {
    @Schema(description = "Institution identifier", example = "d2719de1-0409-4d2e-bf9b-a06f0ea74df7")
    private UUID institutionId;

    @Schema(description = "Institution name", example = "Universidad de San Carlos")
    private String institutionName;

    @Schema(description = "Congress-level rows for this institution")
    private List<EarningsCongressItem> congresses;

    @Schema(description = "Institution total collected amount", example = "2200.00")
    private BigDecimal institutionTotalAmount;

    @Schema(description = "Institution total commission amount", example = "220.00")
    private BigDecimal institutionTotalCommission;

    @Schema(description = "Institution total net amount", example = "1980.00")
    private BigDecimal institutionTotalNet;

    @Schema(description = "Institution total number of payments", example = "40")
    private long paymentCount;
}
