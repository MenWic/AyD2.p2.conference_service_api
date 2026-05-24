package ayd2.p2b.conference_service_api.feature.enrollment.application.register;

import ayd2.p2b.conference_service_api.feature.enrollment.dto.response.EnrollmentResponse;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EnrollParticipantResult {

  EnrollmentResponse enrollment;
  boolean replay;
}
