package ayd2.p2b.conference_service_api.unit.feature.diploma.domain;

import ayd2.p2b.conference_service_api.feature.diploma.domain.exception.DiplomaDomainException;
import ayd2.p2b.conference_service_api.feature.diploma.domain.model.Diploma;
import ayd2.p2b.conference_service_api.feature.diploma.domain.model.DiplomaType;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiplomaTest {

    @Test
    void participationShouldRejectNonNullActivityId() {
        Diploma diploma = baseBuilder()
                .type(DiplomaType.PARTICIPATION)
                .activityId(UUID.randomUUID())
                .build();

        assertThatThrownBy(diploma::validateInvariants).isInstanceOf(DiplomaDomainException.class);
    }

    @Test
    void leadershipShouldRejectNullActivityId() {
        Diploma diploma = baseBuilder()
                .type(DiplomaType.LEADERSHIP)
                .activityId(null)
                .build();

        assertThatThrownBy(diploma::validateInvariants).isInstanceOf(DiplomaDomainException.class);
    }

    @Test
    void requiredFieldsShouldBeEnforced() {
        assertThatThrownBy(() -> baseBuilder().userId(null).build().validateInvariants())
                .isInstanceOf(DiplomaDomainException.class);
        assertThatThrownBy(() -> baseBuilder().congressId(null).build().validateInvariants())
                .isInstanceOf(DiplomaDomainException.class);
        assertThatThrownBy(() -> baseBuilder().type(null).build().validateInvariants())
                .isInstanceOf(DiplomaDomainException.class);
        assertThatThrownBy(() -> baseBuilder().issuedAt(null).build().validateInvariants())
                .isInstanceOf(DiplomaDomainException.class);
        assertThatThrownBy(() -> baseBuilder().createdBy(null).build().validateInvariants())
                .isInstanceOf(DiplomaDomainException.class);
    }

    @Test
    void validParticipationShouldPassInvariants() {
        Diploma diploma = baseBuilder()
                .type(DiplomaType.PARTICIPATION)
                .activityId(null)
                .build();

        assertThatCode(diploma::validateInvariants).doesNotThrowAnyException();
    }

    private Diploma.DiplomaBuilder baseBuilder() {
        return Diploma.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .congressId(UUID.randomUUID())
                .type(DiplomaType.PARTICIPATION)
                .activityId(null)
                .issuedAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .createdBy(UUID.randomUUID())
                .createdAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"));
    }
}
