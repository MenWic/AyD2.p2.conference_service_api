package ayd2.p2b.conference_service_api.unit.feature.attendance.domain;

import ayd2.p2b.conference_service_api.feature.attendance.domain.exception.AttendanceDomainException;
import ayd2.p2b.conference_service_api.feature.attendance.domain.model.Attendance;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttendanceTest {

    @Test
    void shouldValidateInvariantsWhenAttendanceIsValid() {
        Attendance attendance = validAttendanceBuilder().build();

        assertThatNoException().isThrownBy(attendance::validateInvariants);
    }

    @Test
    void shouldRejectNullActivityId() {
        Attendance attendance = validAttendanceBuilder()
                .activityId(null)
                .build();

        assertThatThrownBy(attendance::validateInvariants)
                .isInstanceOf(AttendanceDomainException.class)
                .hasMessageContaining("activityId");
    }

    @Test
    void shouldRejectNullUserId() {
        Attendance attendance = validAttendanceBuilder()
                .userId(null)
                .build();

        assertThatThrownBy(attendance::validateInvariants)
                .isInstanceOf(AttendanceDomainException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void shouldRejectNullPersonalIdSnapshot() {
        Attendance attendance = validAttendanceBuilder()
                .personalIdSnapshot(null)
                .build();

        assertThatThrownBy(attendance::validateInvariants)
                .isInstanceOf(AttendanceDomainException.class)
                .hasMessageContaining("personalIdSnapshot");
    }

    @Test
    void shouldRejectBlankPersonalIdSnapshot() {
        Attendance attendance = validAttendanceBuilder()
                .personalIdSnapshot("   ")
                .build();

        assertThatThrownBy(attendance::validateInvariants)
                .isInstanceOf(AttendanceDomainException.class)
                .hasMessageContaining("personalIdSnapshot");
    }

    @Test
    void shouldRejectNullRegisteredBy() {
        Attendance attendance = validAttendanceBuilder()
                .registeredBy(null)
                .build();

        assertThatThrownBy(attendance::validateInvariants)
                .isInstanceOf(AttendanceDomainException.class)
                .hasMessageContaining("registeredBy");
    }

    @Test
    void shouldRejectNullCreatedBy() {
        Attendance attendance = validAttendanceBuilder()
                .createdBy(null)
                .build();

        assertThatThrownBy(attendance::validateInvariants)
                .isInstanceOf(AttendanceDomainException.class)
                .hasMessageContaining("createdBy");
    }

    private Attendance.AttendanceBuilder validAttendanceBuilder() {
        return Attendance.builder()
                .id(UUID.randomUUID())
                .activityId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .personalIdSnapshot("PID-123")
                .registeredBy(UUID.randomUUID())
                .createdBy(UUID.randomUUID());
    }
}
