package ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentCongressPort;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.internal.CongressEnrollmentSummary;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaEnrollmentCongressAdapter implements EnrollmentCongressPort {

  private final EntityManager entityManager;

  public JpaEnrollmentCongressAdapter(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public Optional<CongressEnrollmentSummary> findPublicEnrollmentCongressById(UUID congressId) {
    Query query = entityManager.createNativeQuery("""
        select c.id, c.institution_id, c.created_by, c.name, i.name, c.price
        from congresses c
        join institutions i on i.id = c.institution_id
        where c.id = :congressId and i.active = true
        """);
    query.setParameter("congressId", congressId);
    return toSummary(query.getResultList());
  }

  @Override
  public Optional<CongressEnrollmentSummary> findManageableCongressById(UUID congressId) {
    Query query = entityManager.createNativeQuery("""
        select c.id, c.institution_id, c.created_by, c.name, i.name, c.price
        from congresses c
        join institutions i on i.id = c.institution_id
        where c.id = :congressId
        """);
    query.setParameter("congressId", congressId);
    return toSummary(query.getResultList());
  }

  @SuppressWarnings("unchecked")
  private Optional<CongressEnrollmentSummary> toSummary(List<?> rows) {
    if (rows.isEmpty()) {
      return Optional.empty();
    }

    Object[] row = (Object[]) rows.getFirst();
    return Optional.of(CongressEnrollmentSummary.builder()
        .congressId((UUID) row[0])
        .institutionId((UUID) row[1])
        .createdBy((UUID) row[2])
        .congressName((String) row[3])
        .institutionName((String) row[4])
        .price((java.math.BigDecimal) row[5])
        .build());
  }
}
