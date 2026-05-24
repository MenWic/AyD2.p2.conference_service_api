package ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressRepositoryPort;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressView;
import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentCongressPort;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.internal.CongressEnrollmentSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaEnrollmentCongressAdapter implements EnrollmentCongressPort {

  private final CongressRepositoryPort congressRepositoryPort;

  @Override
  public Optional<CongressEnrollmentSummary> findCongressSummaryById(UUID congressId) {
    return congressRepositoryPort.findPublicById(congressId)
        .map(this::toSummary);
  }

  private CongressEnrollmentSummary toSummary(CongressView view) {
    return CongressEnrollmentSummary.builder()
        .congressId(view.getCongress().getId())
        .institutionId(view.getCongress().getInstitutionId())
        .congressName(view.getCongress().getName())
        .institutionName(view.getInstitutionName())
        .price(view.getCongress().getPrice())
        .build();
  }
}
