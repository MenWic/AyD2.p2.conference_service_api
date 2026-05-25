package ayd2.p2b.conference_service_api.feature.diploma.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.diploma.application.port.DiplomaEnrollmentPort;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.repository.EnrollmentJpaRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JpaDiplomaEnrollmentAdapter implements DiplomaEnrollmentPort {

    private final EnrollmentJpaRepository enrollmentJpaRepository;

    public JpaDiplomaEnrollmentAdapter(EnrollmentJpaRepository enrollmentJpaRepository) {
        this.enrollmentJpaRepository = enrollmentJpaRepository;
    }

    @Override
    public boolean existsEnrollment(UUID congressId, UUID userId) {
        return enrollmentJpaRepository.existsByCongressIdAndUserId(congressId, userId);
    }
}
