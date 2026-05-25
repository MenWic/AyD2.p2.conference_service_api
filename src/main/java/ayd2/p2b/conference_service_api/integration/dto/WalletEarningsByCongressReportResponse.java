package ayd2.p2b.conference_service_api.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class WalletEarningsByCongressReportResponse {
    private List<WalletEarningsByCongressItem> items;
    private long totalItems;
    private BigDecimal grandTotalAmount;
    private BigDecimal grandTotalCommission;
    private BigDecimal grandTotalNet;
}
