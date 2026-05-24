package ayd2.p2b.conference_service_api.integration.dto;

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
public class WalletPaymentRegisterResponse {

  private UUID id;
  private UUID userId;
  private UUID congressId;
  private BigDecimal amount;
  private LocalDate paymentDate;
}
