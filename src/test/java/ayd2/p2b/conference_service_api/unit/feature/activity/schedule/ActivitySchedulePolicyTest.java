package ayd2.p2b.conference_service_api.unit.feature.activity.schedule;

import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.application.schedule.ActivitySchedulePolicy;
import ayd2.p2b.conference_service_api.feature.activity.domain.exception.ActivityDomainException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActivitySchedulePolicyTest {

    @Test
    void shouldRejectOverlapInSameRoom() {
        ActivityRepositoryPort repositoryPort = mock(ActivityRepositoryPort.class);
        UUID roomId = UUID.randomUUID();
        when(repositoryPort.existsRoomTimeOverlap(roomId,
                OffsetDateTime.parse("2026-10-10T10:00:00Z"),
                OffsetDateTime.parse("2026-10-10T11:00:00Z"),
                null)).thenReturn(true);

        assertThatThrownBy(() -> ActivitySchedulePolicy.ensureNoRoomOverlap(
                repositoryPort,
                roomId,
                OffsetDateTime.parse("2026-10-10T10:00:00Z"),
                OffsetDateTime.parse("2026-10-10T11:00:00Z"),
                null
        )).isInstanceOf(ActivityDomainException.class);
    }

    @Test
    void shouldAllowWhenNoOverlap() {
        ActivityRepositoryPort repositoryPort = mock(ActivityRepositoryPort.class);
        UUID roomId = UUID.randomUUID();
        when(repositoryPort.existsRoomTimeOverlap(roomId,
                OffsetDateTime.parse("2026-10-10T10:00:00Z"),
                OffsetDateTime.parse("2026-10-10T11:00:00Z"),
                null)).thenReturn(false);

        assertThatCode(() -> ActivitySchedulePolicy.ensureNoRoomOverlap(
                repositoryPort,
                roomId,
                OffsetDateTime.parse("2026-10-10T10:00:00Z"),
                OffsetDateTime.parse("2026-10-10T11:00:00Z"),
                null
        )).doesNotThrowAnyException();
    }
}
