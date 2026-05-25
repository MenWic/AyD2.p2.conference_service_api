package ayd2.p2b.conference_service_api.feature.reservation.application.list_by_activity;

import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.reservation.application.exception.ReservationExceptions;
import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationActivityPort;
import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationRepositoryPort;
import ayd2.p2b.conference_service_api.feature.reservation.application.support.ReservationAccessPolicy;
import ayd2.p2b.conference_service_api.feature.reservation.dto.internal.ReservationActivitySummary;
import ayd2.p2b.conference_service_api.feature.reservation.dto.internal.ReservationRequesterContext;
import ayd2.p2b.conference_service_api.feature.reservation.dto.response.ReservationResponse;
import ayd2.p2b.conference_service_api.feature.reservation.mapper.ReservationMapper;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class ListActivityReservationsUseCase {

    private final ReservationRepositoryPort reservationRepositoryPort;
    private final ReservationActivityPort reservationActivityPort;
    private final IamUserLookupPort iamUserLookupPort;
    private final ReservationMapper reservationMapper;

    public ListActivityReservationsUseCase(
            ReservationRepositoryPort reservationRepositoryPort,
            ReservationActivityPort reservationActivityPort,
            IamUserLookupPort iamUserLookupPort,
            ReservationMapper reservationMapper
    ) {
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.reservationActivityPort = reservationActivityPort;
        this.iamUserLookupPort = iamUserLookupPort;
        this.reservationMapper = reservationMapper;
    }

    public PageResponse<ReservationResponse> execute(
            UUID activityId,
            Pageable pageable,
            ReservationRequesterContext requester
    ) {
        ReservationAccessPolicy.ensureCongressAdminScopedWrite(requester);

        ReservationActivitySummary activity = reservationActivityPort.findActivityById(activityId)
                .orElseThrow(() -> ReservationExceptions.activityNotFound(activityId));

        authorizeManageAccess(requester, activity);

        Page<ReservationResponse> responsePage = reservationRepositoryPort.findByActivityId(activityId, pageable)
                .map(reservationMapper::toResponse);

        return PageResponse.<ReservationResponse>builder()
                .items(responsePage.getContent())
                .page(responsePage.getNumber())
                .size(responsePage.getSize())
                .totalItems(responsePage.getTotalElements())
                .totalPages(responsePage.getTotalPages())
                .build();
    }

    private void authorizeManageAccess(
            ReservationRequesterContext requester,
            ReservationActivitySummary activity
    ) {
        if (requester.getUserId().equals(activity.getCongressCreatedBy())) {
            return;
        }

        boolean linked = iamUserLookupPort.isCongressAdminLinkedToInstitution(
                requester.getUserId(),
                activity.getInstitutionId(),
                requester.getAccessToken()
        );
        if (!linked) {
            throw ReservationExceptions.forbidden("Requester is not owner and not linked to congress institution");
        }
    }
}
