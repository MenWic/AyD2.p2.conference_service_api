package ayd2.p2b.conference_service_api.unit.feature.reservation.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.repository.EnrollmentJpaRepository;
import ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.adapter.JpaReservationEnrollmentAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaReservationEnrollmentAdapterTest {

    @Mock
    private EnrollmentJpaRepository enrollmentJpaRepository;

    private JpaReservationEnrollmentAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaReservationEnrollmentAdapter(enrollmentJpaRepository);
    }

    @Test
    void shouldReturnTrueWhenEnrollmentExists() {
        UUID congressId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(enrollmentJpaRepository.existsByCongressIdAndUserId(congressId, userId)).thenReturn(true);

        boolean exists = adapter.existsEnrollment(congressId, userId);

        assertThat(exists).isTrue();
        verify(enrollmentJpaRepository).existsByCongressIdAndUserId(congressId, userId);
    }

    @Test
    void shouldReturnFalseWhenEnrollmentDoesNotExist() {
        UUID congressId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(enrollmentJpaRepository.existsByCongressIdAndUserId(congressId, userId)).thenReturn(false);

        boolean exists = adapter.existsEnrollment(congressId, userId);

        assertThat(exists).isFalse();
        verify(enrollmentJpaRepository).existsByCongressIdAndUserId(congressId, userId);
    }
}
