package ayd2.p2b.conference_service_api.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class WalletInstitutionEarningsItem {
    private UUID institutionId;
    private String institutionName;
    private List<WalletCongressEarningsItem> congresses;
    private BigDecimal institutionTotalAmount;
    private BigDecimal institutionTotalCommission;
    private BigDecimal institutionTotalNet;
    private long paymentCount;
}
