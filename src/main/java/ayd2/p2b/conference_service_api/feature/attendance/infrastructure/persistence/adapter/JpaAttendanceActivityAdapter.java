package ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.attendance.application.port.AttendanceActivityPort;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceActivitySummary;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaAttendanceActivityAdapter implements AttendanceActivityPort {

    private final EntityManager entityManager;

    public JpaAttendanceActivityAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<AttendanceActivitySummary> findActivityById(UUID activityId) {
        Query query = entityManager.createNativeQuery("""
                select a.id, a.congress_id, c.institution_id, c.created_by, a.room_id, a.type
                from activities a
                join congresses c on c.id = a.congress_id
                join institutions i on i.id = c.institution_id
                where a.id = :activityId and i.active = true
                """);
        query.setParameter("activityId", activityId);

        @SuppressWarnings("unchecked")
        var rows = query.getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = (Object[]) rows.getFirst();
        return Optional.of(AttendanceActivitySummary.builder()
                .activityId((UUID) row[0])
                .congressId((UUID) row[1])
                .institutionId((UUID) row[2])
                .congressCreatedBy((UUID) row[3])
                .roomId((UUID) row[4])
                .type(ActivityType.valueOf(row[5].toString()))
                .build());
    }
}
