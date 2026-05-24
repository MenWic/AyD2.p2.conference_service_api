package ayd2.p2b.conference_service_api.feature.enrollment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Enrollment record response")
public class EnrollmentResponse {

  @Schema(description = "Enrollment identifier")
  private UUID id;

  @Schema(description = "Congress the participant enrolled in")
  private UUID congressId;

  @Schema(description = "Enrolled participant user ID")
  private UUID userId;

  @Schema(description = "Associated payment ID in wallet-service")
  private UUID paymentId;

  @Schema(description = "Timestamp when enrollment was recorded")
  private Instant enrolledAt;

  @Schema(description = "Date of payment (YYYY-MM-DD)")
  private LocalDate paymentDate;
}
