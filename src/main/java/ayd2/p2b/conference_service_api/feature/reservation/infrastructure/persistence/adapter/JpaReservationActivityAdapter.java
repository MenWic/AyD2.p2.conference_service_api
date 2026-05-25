package ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationActivityPort;
import ayd2.p2b.conference_service_api.feature.reservation.dto.internal.ReservationActivitySummary;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaReservationActivityAdapter implements ReservationActivityPort {

    private final EntityManager entityManager;

    public JpaReservationActivityAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<ReservationActivitySummary> findActivityById(UUID activityId) {
        return queryActivitySummary(activityId, false);
    }

    @Override
    public Optional<ReservationActivitySummary> findActivityByIdForReservationUpdate(UUID activityId) {
        return queryActivitySummary(activityId, true);
    }

    private Optional<ReservationActivitySummary> queryActivitySummary(UUID activityId, boolean lockForUpdate) {
        String sql = """
                select a.id, a.congress_id, c.institution_id, c.created_by, a.type, a.workshop_capacity
                from activities a
                join congresses c on c.id = a.congress_id
                join institutions i on i.id = c.institution_id
                where a.id = :activityId and i.active = true
                """;
        if (lockForUpdate) {
            sql = sql + " for update of a";
        }

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("activityId", activityId);

        @SuppressWarnings("unchecked")
        var rows = query.getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = (Object[]) rows.getFirst();
        return Optional.of(ReservationActivitySummary.builder()
                .activityId((UUID) row[0])
                .congressId((UUID) row[1])
                .institutionId((UUID) row[2])
                .congressCreatedBy((UUID) row[3])
                .type(ActivityType.valueOf(row[4].toString()))
                .workshopCapacity((Integer) row[5])
                .build());
    }
}
