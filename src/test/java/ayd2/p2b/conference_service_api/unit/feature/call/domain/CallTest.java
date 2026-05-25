package ayd2.p2b.conference_service_api.unit.feature.call.domain;

import ayd2.p2b.conference_service_api.feature.call.domain.exception.CallDomainException;
import ayd2.p2b.conference_service_api.feature.call.domain.model.Call;
import ayd2.p2b.conference_service_api.feature.call.domain.model.CallStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CallTest {

    @Test
    void shouldAcceptOpenCallWithNullClosedAt() {
        Call call = baseCallBuilder()
                .status(CallStatus.OPEN)
                .closedAt(null)
                .build();

        assertThatCode(call::validateInvariants).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectNullStatus() {
        Call call = baseCallBuilder()
                .status(null)
                .build();

        assertThatThrownBy(call::validateInvariants).isInstanceOf(CallDomainException.class);
    }

    @Test
    void shouldRejectOpenCallWithClosedAt() {
        Call call = baseCallBuilder()
                .status(CallStatus.OPEN)
                .closedAt(OffsetDateTime.now())
                .build();

        assertThatThrownBy(call::validateInvariants).isInstanceOf(CallDomainException.class);
    }

    @Test
    void shouldRejectClosedCallWithoutClosedAt() {
        Call call = baseCallBuilder()
                .status(CallStatus.CLOSED)
                .closedAt(null)
                .build();

        assertThatThrownBy(call::validateInvariants).isInstanceOf(CallDomainException.class);
    }

    @Test
    void shouldCloseOpenCall() {
        UUID updaterId = UUID.randomUUID();
        OffsetDateTime closedAt = OffsetDateTime.parse("2026-10-10T12:00:00Z");
        Call openCall = baseCallBuilder()
                .status(CallStatus.OPEN)
                .closedAt(null)
                .build();

        Call closed = openCall.close(updaterId, closedAt);

        assertThat(closed.getStatus()).isEqualTo(CallStatus.CLOSED);
        assertThat(closed.getClosedAt()).isEqualTo(closedAt);
        assertThat(closed.getUpdatedBy()).isEqualTo(updaterId);
    }

    @Test
    void shouldRejectCloseOnAlreadyClosedCall() {
        Call closedCall = baseCallBuilder()
                .status(CallStatus.CLOSED)
                .closedAt(OffsetDateTime.parse("2026-10-10T12:00:00Z"))
                .build();

        assertThatThrownBy(() -> closedCall.close(UUID.randomUUID(), OffsetDateTime.now()))
                .isInstanceOf(CallDomainException.class);
    }

    @Test
    void shouldRejectCloseWithNullClosedAt() {
        Call openCall = baseCallBuilder()
                .status(CallStatus.OPEN)
                .closedAt(null)
                .build();

        assertThatThrownBy(() -> openCall.close(UUID.randomUUID(), null))
                .isInstanceOf(CallDomainException.class);
    }

    private Call.CallBuilder baseCallBuilder() {
        return Call.builder()
                .id(UUID.randomUUID())
                .congressId(UUID.randomUUID())
                .status(CallStatus.OPEN)
                .openedAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .createdBy(UUID.randomUUID());
    }
}
