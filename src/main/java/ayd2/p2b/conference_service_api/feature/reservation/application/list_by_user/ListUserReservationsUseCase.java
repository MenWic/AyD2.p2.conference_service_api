package ayd2.p2b.conference_service_api.feature.reservation.application.list_by_user;

import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationRepositoryPort;
import ayd2.p2b.conference_service_api.feature.reservation.application.support.ReservationAccessPolicy;
import ayd2.p2b.conference_service_api.feature.reservation.dto.internal.ReservationRequesterContext;
import ayd2.p2b.conference_service_api.feature.reservation.dto.response.ReservationResponse;
import ayd2.p2b.conference_service_api.feature.reservation.mapper.ReservationMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class ListUserReservationsUseCase {

    private final ReservationRepositoryPort reservationRepositoryPort;
    private final ReservationMapper reservationMapper;

    public ListUserReservationsUseCase(
            ReservationRepositoryPort reservationRepositoryPort,
            ReservationMapper reservationMapper
    ) {
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.reservationMapper = reservationMapper;
    }

    public PageResponse<ReservationResponse> execute(
            UUID requestedUserId,
            Pageable pageable,
            ReservationRequesterContext requester
    ) {
        ReservationAccessPolicy.ensureParticipant(requester);
        ReservationAccessPolicy.ensureSelfAccess(requestedUserId, requester);

        Page<ReservationResponse> responsePage = reservationRepositoryPort.findByUserId(requestedUserId, pageable)
                .map(reservationMapper::toResponse);

        return PageResponse.<ReservationResponse>builder()
                .items(responsePage.getContent())
                .page(responsePage.getNumber())
                .size(responsePage.getSize())
                .totalItems(responsePage.getTotalElements())
                .totalPages(responsePage.getTotalPages())
                .build();
    }
}
