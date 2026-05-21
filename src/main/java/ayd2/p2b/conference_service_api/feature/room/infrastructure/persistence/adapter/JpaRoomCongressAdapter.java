package ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.room.application.port.RoomCongressPort;
import ayd2.p2b.conference_service_api.feature.room.dto.internal.RoomCongressSummary;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaRoomCongressAdapter implements RoomCongressPort {

    private final EntityManager entityManager;

    public JpaRoomCongressAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<RoomCongressSummary> findManageableCongressById(UUID congressId) {
        Query query = entityManager.createNativeQuery("""
                select c.id, c.institution_id, c.created_by
                from congresses c
                join institutions i on i.id = c.institution_id
                where c.id = :congressId and i.active = true
                """);
        query.setParameter("congressId", congressId);

        @SuppressWarnings("unchecked")
        var rows = query.getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = (Object[]) rows.getFirst();
        return Optional.of(RoomCongressSummary.builder()
                .id((UUID) row[0])
                .institutionId((UUID) row[1])
                .createdBy((UUID) row[2])
                .build());
    }

    @Override
    public boolean existsPublicCongressById(UUID congressId) {
        Query query = entityManager.createNativeQuery("""
                select exists(
                    select 1
                    from congresses c
                    join institutions i on i.id = c.institution_id
                    where c.id = :congressId and i.active = true
                )
                """);
        query.setParameter("congressId", congressId);
        Object value = query.getSingleResult();
        return Boolean.TRUE.equals(value);
    }
}
