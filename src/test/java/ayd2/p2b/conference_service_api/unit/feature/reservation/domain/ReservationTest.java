package ayd2.p2b.conference_service_api.unit.feature.reservation.domain;

import ayd2.p2b.conference_service_api.feature.reservation.domain.exception.ReservationDomainException;
import ayd2.p2b.conference_service_api.feature.reservation.domain.model.Reservation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    @Test
    void shouldValidateInvariantsWhenReservationIsValid() {
        Reservation reservation = validReservationBuilder().build();

        assertThatNoException().isThrownBy(reservation::validateInvariants);
    }

    @Test
    void shouldRejectNullActivityId() {
        Reservation reservation = validReservationBuilder()
                .activityId(null)
                .build();

        assertThatThrownBy(reservation::validateInvariants)
                .isInstanceOf(ReservationDomainException.class)
                .hasMessageContaining("activityId");
    }

    @Test
    void shouldRejectNullUserId() {
        Reservation reservation = validReservationBuilder()
                .userId(null)
                .build();

        assertThatThrownBy(reservation::validateInvariants)
                .isInstanceOf(ReservationDomainException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void shouldRejectNullCreatedBy() {
        Reservation reservation = validReservationBuilder()
                .createdBy(null)
                .build();

        assertThatThrownBy(reservation::validateInvariants)
                .isInstanceOf(ReservationDomainException.class)
                .hasMessageContaining("createdBy");
    }

    private Reservation.ReservationBuilder validReservationBuilder() {
        return Reservation.builder()
                .id(UUID.randomUUID())
                .activityId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .createdBy(UUID.randomUUID());
    }
}
