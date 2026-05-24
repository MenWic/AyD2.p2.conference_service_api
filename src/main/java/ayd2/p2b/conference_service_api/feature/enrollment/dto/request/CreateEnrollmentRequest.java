package ayd2.p2b.conference_service_api.feature.enrollment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to enroll a participant in a congress")
public class CreateEnrollmentRequest {

  @NotNull(message = "paymentDate is required")
  @Schema(description = "Date of payment (YYYY-MM-DD)", required = true)
  private LocalDate paymentDate;
}
