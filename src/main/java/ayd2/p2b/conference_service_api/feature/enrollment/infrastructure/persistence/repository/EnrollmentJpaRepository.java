package ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.repository;

import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.entity.EnrollmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EnrollmentJpaRepository extends JpaRepository<EnrollmentEntity, UUID> {

  Optional<EnrollmentEntity> findByCongressIdAndUserId(UUID congressId, UUID userId);

  boolean existsByCongressIdAndUserId(UUID congressId, UUID userId);

  Page<EnrollmentEntity> findByUserId(UUID userId, Pageable pageable);

  Page<EnrollmentEntity> findByCongressId(UUID congressId, Pageable pageable);
}
