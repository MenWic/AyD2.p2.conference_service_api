package ayd2.p2b.conference_service_api.feature.report.infrastructure.persistence.query;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.report.application.workshop_reservations.port.WorkshopReservationsQueryPort;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.WorkshopReservationRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JpaWorkshopReservationsQuery implements WorkshopReservationsQueryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<WorkshopReservationRow> query(UUID congressId, UUID activityIdFilter) {
        StringBuilder jpql = new StringBuilder("""
                select a.id, a.name, a.workshopCapacity, r.userId
                from ActivityEntity a
                left join ReservationEntity r on r.activityId = a.id
                where a.congressId = :congressId
                  and a.type = :workshopType
                """);

        if (activityIdFilter != null) {
            jpql.append(" and a.id = :activityId");
        }

        jpql.append(" order by a.startTime asc, a.name asc, r.userId asc");

        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class)
                .setParameter("congressId", congressId)
                .setParameter("workshopType", ActivityType.TALLER);

        if (activityIdFilter != null) {
            query.setParameter("activityId", activityIdFilter);
        }

        Map<UUID, WorkshopReservationRowAccumulator> grouped = new LinkedHashMap<>();
        query.getResultList().forEach(row -> {
            UUID activityId = (UUID) row[0];
            String activityName = (String) row[1];
            Integer workshopCapacity = (Integer) row[2];
            UUID reservedUserId = (UUID) row[3];

            WorkshopReservationRowAccumulator accumulator = grouped.computeIfAbsent(
                    activityId,
                    ignored -> new WorkshopReservationRowAccumulator(
                            activityId,
                            activityName,
                            workshopCapacity == null ? 0 : workshopCapacity
                    ));

            if (reservedUserId != null) {
                accumulator.reservedUserIds.add(reservedUserId);
            }
        });

        return grouped.values().stream()
                .map(acc -> WorkshopReservationRow.builder()
                        .activityId(acc.activityId)
                        .activityName(acc.activityName)
                        .workshopCapacity(acc.workshopCapacity)
                        .reservedUserIds(List.copyOf(acc.reservedUserIds))
                        .build())
                .toList();
    }

    private static final class WorkshopReservationRowAccumulator {
        private final UUID activityId;
        private final String activityName;
        private final int workshopCapacity;
        private final List<UUID> reservedUserIds = new ArrayList<>();

        private WorkshopReservationRowAccumulator(UUID activityId, String activityName, int workshopCapacity) {
            this.activityId = activityId;
            this.activityName = activityName;
            this.workshopCapacity = workshopCapacity;
        }
    }
}
