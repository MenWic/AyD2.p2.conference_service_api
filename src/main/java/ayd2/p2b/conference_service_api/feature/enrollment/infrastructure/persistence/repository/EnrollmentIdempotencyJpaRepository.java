package ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.repository;

import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.entity.EnrollmentIdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentIdempotencyJpaRepository
    extends JpaRepository<EnrollmentIdempotencyRecordEntity, String> {

  @Query("""
      SELECT e FROM EnrollmentIdempotencyRecordEntity e
      WHERE e.congressId = :congressId AND e.userId = :userId
        AND e.status IN ('PROCESSING', 'SUCCEEDED')
      """)
  Optional<EnrollmentIdempotencyRecordEntity> findActiveByCongressIdAndUserId(
      @Param("congressId") UUID congressId,
      @Param("userId") UUID userId);
}
