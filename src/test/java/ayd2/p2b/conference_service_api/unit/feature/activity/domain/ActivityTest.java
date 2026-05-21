package ayd2.p2b.conference_service_api.unit.feature.activity.domain;

import ayd2.p2b.conference_service_api.feature.activity.domain.exception.ActivityDomainException;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.Activity;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivityTest {

    @Test
    void shouldRejectStartTimeGreaterOrEqualThanEndTime() {
        Activity activity = baseActivityBuilder()
                .startTime(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .endTime(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .build();

        assertThatThrownBy(activity::validateInvariants)
                .isInstanceOf(ActivityDomainException.class);
    }

    @Test
    void shouldRejectTallerWithoutPositiveWorkshopCapacity() {
        Activity activity = baseActivityBuilder()
                .type(ActivityType.TALLER)
                .workshopCapacity(0)
                .build();

        assertThatThrownBy(activity::validateInvariants)
                .isInstanceOf(ActivityDomainException.class);
    }

    @Test
    void shouldRejectPonenciaWithWorkshopCapacity() {
        Activity activity = baseActivityBuilder()
                .type(ActivityType.PONENCIA)
                .workshopCapacity(20)
                .build();

        assertThatThrownBy(activity::validateInvariants)
                .isInstanceOf(ActivityDomainException.class);
    }

    @Test
    void shouldAcceptValidTaller() {
        Activity activity = baseActivityBuilder()
                .type(ActivityType.TALLER)
                .workshopCapacity(20)
                .build();

        assertThatCode(activity::validateInvariants).doesNotThrowAnyException();
    }

    private Activity.ActivityBuilder baseActivityBuilder() {
        return Activity.builder()
                .id(UUID.randomUUID())
                .congressId(UUID.randomUUID())
                .roomId(UUID.randomUUID())
                .name("Taller")
                .description("Descripcion")
                .type(ActivityType.PONENCIA)
                .startTime(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .endTime(OffsetDateTime.parse("2026-10-10T11:00:00Z"))
                .workshopCapacity(null)
                .createdBy(UUID.randomUUID());
    }
}
