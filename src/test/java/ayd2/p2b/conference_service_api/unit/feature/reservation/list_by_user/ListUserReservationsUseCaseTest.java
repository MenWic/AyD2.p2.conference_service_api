package ayd2.p2b.conference_service_api.unit.feature.reservation.list_by_user;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.reservation.application.list_by_user.ListUserReservationsUseCase;
import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationRepositoryPort;
import ayd2.p2b.conference_service_api.feature.reservation.domain.model.Reservation;
import ayd2.p2b.conference_service_api.feature.reservation.dto.internal.ReservationRequesterContext;
import ayd2.p2b.conference_service_api.feature.reservation.dto.response.ReservationResponse;
import ayd2.p2b.conference_service_api.feature.reservation.mapper.ReservationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListUserReservationsUseCaseTest {

    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;
    @Mock
    private ReservationMapper reservationMapper;

    private ListUserReservationsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListUserReservationsUseCase(reservationRepositoryPort, reservationMapper);
    }

    @Test
    void shouldAllowSelfParticipantToListReservations() {
        UUID userId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        Reservation reservation = Reservation.builder()
                .id(UUID.randomUUID())
                .activityId(UUID.randomUUID())
                .userId(userId)
                .reservedAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .createdBy(userId)
                .build();
        ReservationResponse response = ReservationResponse.builder()
                .id(reservation.getId())
                .activityId(reservation.getActivityId())
                .userId(userId)
                .reservedAt(reservation.getReservedAt())
                .build();

        when(reservationRepositoryPort.findByUserId(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(reservation), pageable, 1));
        when(reservationMapper.toResponse(reservation)).thenReturn(response);

        PageResponse<ReservationResponse> page = useCase.execute(userId, pageable, requester(userId));

        verify(reservationRepositoryPort).findByUserId(userId, pageable);
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getItems().getFirst().getUserId()).isEqualTo(userId);
    }

    @Test
    void shouldRejectWhenUserRequestsAnotherUsersReservations() {
        UUID requesterId = UUID.randomUUID();
        UUID pathUserId = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.execute(pathUserId, PageRequest.of(0, 20), requester(requesterId)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
                });
    }

    private ReservationRequesterContext requester(UUID userId) {
        return ReservationRequesterContext.builder()
                .userId(userId)
                .roles(Set.of(Role.PARTICIPANT))
                .accessToken("token")
                .build();
    }
}
