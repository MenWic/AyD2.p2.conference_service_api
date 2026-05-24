package ayd2.p2b.conference_service_api.feature.enrollment.application.port;

import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepositoryPort {

  Enrollment save(Enrollment enrollment);

  Optional<Enrollment> findByCongressIdAndUserId(UUID congressId, UUID userId);

  Page<Enrollment> findByUserId(UUID userId, Pageable pageable);

  Page<Enrollment> findByCongressId(UUID congressId, Pageable pageable);
}
