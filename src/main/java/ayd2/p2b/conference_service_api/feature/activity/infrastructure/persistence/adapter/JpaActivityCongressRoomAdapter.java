package ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityCongressRoomPort;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivityCongressRoomSummary;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaActivityCongressRoomAdapter implements ActivityCongressRoomPort {

    private final EntityManager entityManager;

    public JpaActivityCongressRoomAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<ActivityCongressRoomSummary> findManageableCongressById(UUID congressId) {
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
        return Optional.of(ActivityCongressRoomSummary.builder()
                .congressId((UUID) row[0])
                .institutionId((UUID) row[1])
                .createdBy((UUID) row[2])
                .build());
    }

    @Override
    public Optional<ActivityCongressRoomSummary> findManageableRoomById(UUID roomId) {
        Query query = entityManager.createNativeQuery("""
                select r.id, c.id, c.institution_id, c.created_by
                from rooms r
                join congresses c on c.id = r.congress_id
                join institutions i on i.id = c.institution_id
                where r.id = :roomId and i.active = true
                """);
        query.setParameter("roomId", roomId);

        @SuppressWarnings("unchecked")
        var rows = query.getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = (Object[]) rows.getFirst();
        return Optional.of(ActivityCongressRoomSummary.builder()
                .roomId((UUID) row[0])
                .congressId((UUID) row[1])
                .institutionId((UUID) row[2])
                .createdBy((UUID) row[3])
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
