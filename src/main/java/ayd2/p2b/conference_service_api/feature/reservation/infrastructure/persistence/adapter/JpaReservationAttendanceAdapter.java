package ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationAttendancePort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JpaReservationAttendanceAdapter implements ReservationAttendancePort {

    private final EntityManager entityManager;

    public JpaReservationAttendanceAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsAttendance(UUID activityId, UUID userId) {
        Query query = entityManager.createNativeQuery("""
                select exists(
                    select 1
                    from attendances at
                    where at.activity_id = :activityId
                      and at.user_id = :userId
                )
                """);
        query.setParameter("activityId", activityId);
        query.setParameter("userId", userId);
        Object value = query.getSingleResult();
        return Boolean.TRUE.equals(value);
    }
}
