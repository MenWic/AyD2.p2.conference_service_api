package ayd2.p2b.conference_service_api.feature.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "One congress earnings row for CongressAdmin financial report")
public class EarningsByCongressItem {
    @Schema(description = "Congress identifier", example = "7d899e63-481d-4df8-87f1-7a8d8f437b68")
    private UUID congressId;

    @Schema(description = "Congress name", example = "Congreso Nacional de Ingenieria")
    private String congressName;

    @Schema(description = "Total collected amount", example = "1200.00")
    private BigDecimal totalAmount;

    @Schema(description = "Commission amount", example = "120.00")
    private BigDecimal commissionAmount;

    @Schema(description = "Net amount", example = "1080.00")
    private BigDecimal netAmount;

    @Schema(description = "Total number of payments", example = "24")
    private long paymentCount;
}
