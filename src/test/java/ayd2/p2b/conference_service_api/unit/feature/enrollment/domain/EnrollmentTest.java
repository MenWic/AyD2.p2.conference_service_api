package ayd2.p2b.conference_service_api.unit.feature.enrollment.domain;

import ayd2.p2b.conference_service_api.feature.enrollment.domain.exception.EnrollmentDomainException;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.Enrollment;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnrollmentTest {

    @Test
    void shouldAcceptValidEnrollment() {
        Enrollment enrollment = validEnrollmentBuilder().build();

        assertThatCode(enrollment::validateInvariants).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectNullCongressId() {
        Enrollment enrollment = validEnrollmentBuilder()
                .congressId(null)
                .build();

        assertThatThrownBy(enrollment::validateInvariants).isInstanceOf(EnrollmentDomainException.class);
    }

    @Test
    void shouldRejectNullUserId() {
        Enrollment enrollment = validEnrollmentBuilder()
                .userId(null)
                .build();

        assertThatThrownBy(enrollment::validateInvariants).isInstanceOf(EnrollmentDomainException.class);
    }

    @Test
    void shouldRejectNullPaymentId() {
        Enrollment enrollment = validEnrollmentBuilder()
                .paymentId(null)
                .build();

        assertThatThrownBy(enrollment::validateInvariants).isInstanceOf(EnrollmentDomainException.class);
    }

    @Test
    void shouldRejectNullPaymentDate() {
        Enrollment enrollment = validEnrollmentBuilder()
                .paymentDate(null)
                .build();

        assertThatThrownBy(enrollment::validateInvariants).isInstanceOf(EnrollmentDomainException.class);
    }

    @Test
    void shouldRejectNullCreatedBy() {
        Enrollment enrollment = validEnrollmentBuilder()
                .createdBy(null)
                .build();

        assertThatThrownBy(enrollment::validateInvariants).isInstanceOf(EnrollmentDomainException.class);
    }

    private Enrollment.EnrollmentBuilder validEnrollmentBuilder() {
        return Enrollment.builder()
                .congressId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .paymentDate(LocalDate.of(2026, 10, 10))
                .createdBy(UUID.randomUUID());
    }
}
