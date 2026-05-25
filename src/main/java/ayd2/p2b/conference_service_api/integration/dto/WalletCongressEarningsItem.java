package ayd2.p2b.conference_service_api.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class WalletCongressEarningsItem {
    private UUID congressId;
    private String congressName;
    private BigDecimal totalAmount;
    private BigDecimal commissionAmount;
    private BigDecimal netAmount;
    private long paymentCount;
}
