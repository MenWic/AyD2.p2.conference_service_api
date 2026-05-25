package ayd2.p2b.conference_service_api.feature.report.infrastructure.persistence.query;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityEntity;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository.ActivityRepository;
import ayd2.p2b.conference_service_api.feature.report.application.workshop_reservations.port.WorkshopReservationsQueryPort;
import ayd2.p2b.conference_service_api.feature.report.dto.response.RosterEntry;
import ayd2.p2b.conference_service_api.feature.report.dto.response.WorkshopReservationItem;
import ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.repository.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaWorkshopReservationsQuery implements WorkshopReservationsQueryPort {

    private final ActivityRepository activityRepository;
    private final ReservationJpaRepository reservationRepository;

    @Override
    public List<WorkshopReservationItem> query(UUID congressId, UUID activityIdFilter) {
        return activityRepository.findAll().stream()
                .filter(a -> congressId.equals(a.getCongressId()))
                .filter(a -> ActivityType.TALLER.equals(a.getType()))
                .filter(a -> activityIdFilter == null || activityIdFilter.equals(a.getId()))
                .map(a -> buildItem(a))
                .toList();
    }

    private WorkshopReservationItem buildItem(ActivityEntity activity) {
        int capacity = activity.getWorkshopCapacity() != null ? activity.getWorkshopCapacity() : 0;
        List<UUID> reservedUserIds = reservationRepository.findByActivityId(activity.getId())
                .stream().map(r -> r.getUserId()).toList();
        int reservationCount = reservedUserIds.size();
        int available = Math.max(0, capacity - reservationCount);

        List<RosterEntry> roster = reservedUserIds.stream()
                .map(uid -> RosterEntry.builder()
                        .personalId(uid.toString())
                        .fullName(null)
                        .email(null)
                        .build())
                .toList();

        return WorkshopReservationItem.builder()
                .activityId(activity.getId())
                .activityName(activity.getName())
                .workshopCapacity(capacity)
                .reservationCount(reservationCount)
                .availableSeats(available)
                .roster(roster)
                .build();
    }
}
