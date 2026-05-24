package ayd2.p2b.conference_service_api.feature.enrollment.dto.internal;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class CongressEnrollmentSummary {

  UUID congressId;
  UUID institutionId;
  String congressName;
  String institutionName;
  BigDecimal price;
}
