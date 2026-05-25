package ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.attendance.application.port.AttendanceReservationPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JpaAttendanceReservationAdapter implements AttendanceReservationPort {

    private final EntityManager entityManager;

    public JpaAttendanceReservationAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsReservation(UUID activityId, UUID userId) {
        Query query = entityManager.createNativeQuery("""
                select exists(
                    select 1
                    from reservations r
                    where r.activity_id = :activityId
                      and r.user_id = :userId
                )
                """);
        query.setParameter("activityId", activityId);
        query.setParameter("userId", userId);
        Object value = query.getSingleResult();
        return Boolean.TRUE.equals(value);
    }
}
