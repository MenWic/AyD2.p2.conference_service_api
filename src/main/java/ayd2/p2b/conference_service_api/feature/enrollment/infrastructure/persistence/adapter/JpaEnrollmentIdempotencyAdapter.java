package ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentIdempotencyRepositoryPort;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.EnrollmentIdempotencyRecord;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.entity.EnrollmentIdempotencyRecordEntity;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.repository.EnrollmentIdempotencyJpaRepository;
import ayd2.p2b.conference_service_api.feature.enrollment.mapper.EnrollmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaEnrollmentIdempotencyAdapter implements EnrollmentIdempotencyRepositoryPort {

  private final EnrollmentIdempotencyJpaRepository idempotencyJpaRepository;
  private final EnrollmentMapper enrollmentMapper;

  @Override
  public EnrollmentIdempotencyRecord insert(EnrollmentIdempotencyRecord record) {
    try {
      EnrollmentIdempotencyRecordEntity entity = enrollmentMapper.toIdempotencyEntity(record);
      EnrollmentIdempotencyRecordEntity saved = idempotencyJpaRepository.saveAndFlush(entity);
      return enrollmentMapper.toIdempotencyDomain(saved);
    } catch (DataIntegrityViolationException ex) {
      throw new ApiException(HttpStatus.CONFLICT, "resource.conflict",
          "An active enrollment already exists for this congress and user");
    }
  }

  @Override
  public Optional<EnrollmentIdempotencyRecord> findByKey(String idempotencyKey) {
    return idempotencyJpaRepository.findById(idempotencyKey)
        .map(enrollmentMapper::toIdempotencyDomain);
  }

  @Override
  public Optional<EnrollmentIdempotencyRecord> findActiveByCongressIdAndUserId(UUID congressId, UUID userId) {
    return idempotencyJpaRepository.findActiveByCongressIdAndUserId(congressId, userId)
        .map(enrollmentMapper::toIdempotencyDomain);
  }

  @Override
  public EnrollmentIdempotencyRecord update(EnrollmentIdempotencyRecord record) {
    EnrollmentIdempotencyRecordEntity entity = enrollmentMapper.toIdempotencyEntity(record);
    EnrollmentIdempotencyRecordEntity saved = idempotencyJpaRepository.save(entity);
    return enrollmentMapper.toIdempotencyDomain(saved);
  }
}
