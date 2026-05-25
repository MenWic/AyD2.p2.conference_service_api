package ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.attendance.application.port.AttendanceEnrollmentPort;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.repository.EnrollmentJpaRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JpaAttendanceEnrollmentAdapter implements AttendanceEnrollmentPort {

    private final EnrollmentJpaRepository enrollmentJpaRepository;

    public JpaAttendanceEnrollmentAdapter(EnrollmentJpaRepository enrollmentJpaRepository) {
        this.enrollmentJpaRepository = enrollmentJpaRepository;
    }

    @Override
    public boolean existsEnrollment(UUID congressId, UUID userId) {
        return enrollmentJpaRepository.existsByCongressIdAndUserId(congressId, userId);
    }
}
