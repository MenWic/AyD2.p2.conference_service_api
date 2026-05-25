package ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.repository.EnrollmentJpaRepository;
import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationEnrollmentPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JpaReservationEnrollmentAdapter implements ReservationEnrollmentPort {

    private final EnrollmentJpaRepository enrollmentJpaRepository;

    public JpaReservationEnrollmentAdapter(EnrollmentJpaRepository enrollmentJpaRepository) {
        this.enrollmentJpaRepository = enrollmentJpaRepository;
    }

    @Override
    public boolean existsEnrollment(UUID congressId, UUID userId) {
        return enrollmentJpaRepository.existsByCongressIdAndUserId(congressId, userId);
    }
}
