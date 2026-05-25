package ayd2.p2b.conference_service_api.unit.feature.reservation.list_by_activity;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.reservation.application.list_by_activity.ListActivityReservationsUseCase;
import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationActivityPort;
import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationRepositoryPort;
import ayd2.p2b.conference_service_api.feature.reservation.domain.model.Reservation;
import ayd2.p2b.conference_service_api.feature.reservation.dto.internal.ReservationActivitySummary;
import ayd2.p2b.conference_service_api.feature.reservation.dto.internal.ReservationRequesterContext;
import ayd2.p2b.conference_service_api.feature.reservation.dto.response.ReservationResponse;
import ayd2.p2b.conference_service_api.feature.reservation.mapper.ReservationMapper;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListActivityReservationsUseCaseTest {

    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;
    @Mock
    private ReservationActivityPort reservationActivityPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;
    @Mock
    private ReservationMapper reservationMapper;

    private ListActivityReservationsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListActivityReservationsUseCase(
                reservationRepositoryPort,
                reservationActivityPort,
                iamUserLookupPort,
                reservationMapper
        );
    }

    @Test
    void shouldAllowOwnerCongressAdminToListActivityReservations() {
        UUID activityId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ReservationActivitySummary activity = activitySummary(activityId, ownerId);
        Reservation reservation = reservation(activityId, UUID.randomUUID());
        ReservationResponse response = response(reservation);
        PageRequest pageable = PageRequest.of(0, 20);

        when(reservationActivityPort.findActivityById(activityId)).thenReturn(Optional.of(activity));
        when(reservationRepositoryPort.findByActivityId(activityId, pageable))
                .thenReturn(new PageImpl<>(List.of(reservation), pageable, 1));
        when(reservationMapper.toResponse(reservation)).thenReturn(response);

        PageResponse<ReservationResponse> page = useCase.execute(
                activityId,
                pageable,
                requester(ownerId)
        );

        verify(iamUserLookupPort, never()).isCongressAdminLinkedToInstitution(any(), any(), any());
        assertThat(page.getItems()).hasSize(1);
    }

    @Test
    void shouldAllowScopedCongressAdminToListActivityReservations() {
        UUID activityId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        ReservationActivitySummary activity = activitySummary(activityId, UUID.randomUUID());
        PageRequest pageable = PageRequest.of(0, 20);

        when(reservationActivityPort.findActivityById(activityId)).thenReturn(Optional.of(activity));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(
                requesterId,
                activity.getInstitutionId(),
                "token")).thenReturn(true);
        when(reservationRepositoryPort.findByActivityId(activityId, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<ReservationResponse> page = useCase.execute(
                activityId,
                pageable,
                requester(requesterId)
        );

        assertThat(page.getTotalItems()).isZero();
    }

    @Test
    void shouldRejectUnscopedCongressAdmin() {
        UUID activityId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        ReservationActivitySummary activity = activitySummary(activityId, UUID.randomUUID());

        when(reservationActivityPort.findActivityById(activityId)).thenReturn(Optional.of(activity));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(
                requesterId,
                activity.getInstitutionId(),
                "token")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(activityId, PageRequest.of(0, 20), requester(requesterId)))
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
                .roles(Set.of(Role.CONGRESS_ADMIN, Role.PARTICIPANT))
                .accessToken("token")
                .build();
    }

    private ReservationActivitySummary activitySummary(UUID activityId, UUID ownerId) {
        return ReservationActivitySummary.builder()
                .activityId(activityId)
                .congressId(UUID.randomUUID())
                .institutionId(UUID.randomUUID())
                .congressCreatedBy(ownerId)
                .type(ActivityType.TALLER)
                .workshopCapacity(20)
                .build();
    }

    private Reservation reservation(UUID activityId, UUID userId) {
        return Reservation.builder()
                .id(UUID.randomUUID())
                .activityId(activityId)
                .userId(userId)
                .reservedAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .createdBy(userId)
                .build();
    }

    private ReservationResponse response(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .activityId(reservation.getActivityId())
                .userId(reservation.getUserId())
                .reservedAt(reservation.getReservedAt())
                .build();
    }
}
