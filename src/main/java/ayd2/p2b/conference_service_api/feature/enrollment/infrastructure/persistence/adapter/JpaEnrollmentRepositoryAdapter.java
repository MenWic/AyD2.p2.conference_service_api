package ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentRepositoryPort;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.Enrollment;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.entity.EnrollmentEntity;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.repository.EnrollmentJpaRepository;
import ayd2.p2b.conference_service_api.feature.enrollment.mapper.EnrollmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaEnrollmentRepositoryAdapter implements EnrollmentRepositoryPort {

  private final EnrollmentJpaRepository enrollmentJpaRepository;
  private final EnrollmentMapper enrollmentMapper;

  @Override
  public Enrollment save(Enrollment enrollment) {
    EnrollmentEntity entity = enrollmentMapper.toEntity(enrollment);
    EnrollmentEntity saved = enrollmentJpaRepository.save(entity);
    return enrollmentMapper.toDomain(saved);
  }

  @Override
  public Optional<Enrollment> findByCongressIdAndUserId(UUID congressId, UUID userId) {
    return enrollmentJpaRepository.findByCongressIdAndUserId(congressId, userId)
        .map(enrollmentMapper::toDomain);
  }

  @Override
  public Page<Enrollment> findByUserId(UUID userId, Pageable pageable) {
    return enrollmentJpaRepository.findByUserId(userId, pageable)
        .map(enrollmentMapper::toDomain);
  }

  @Override
  public Page<Enrollment> findByCongressId(UUID congressId, Pageable pageable) {
    return enrollmentJpaRepository.findByCongressId(congressId, pageable)
        .map(enrollmentMapper::toDomain);
  }
}
