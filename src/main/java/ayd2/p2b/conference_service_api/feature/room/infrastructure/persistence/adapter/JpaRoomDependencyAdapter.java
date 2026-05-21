package ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.room.application.port.RoomDependencyPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JpaRoomDependencyAdapter implements RoomDependencyPort {

    private final EntityManager entityManager;

    public JpaRoomDependencyAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsActivitiesForRoom(UUID roomId) {
        Query query = entityManager.createNativeQuery("""
                select exists(select 1 from activities a where a.room_id = :roomId)
                """);
        query.setParameter("roomId", roomId);
        Object value = query.getSingleResult();
        return Boolean.TRUE.equals(value);
    }
}
