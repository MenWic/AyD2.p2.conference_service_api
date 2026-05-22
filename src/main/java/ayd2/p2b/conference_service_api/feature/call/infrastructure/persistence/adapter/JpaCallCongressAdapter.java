package ayd2.p2b.conference_service_api.feature.call.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.call.application.port.CallCongressPort;
import ayd2.p2b.conference_service_api.feature.call.dto.internal.CallCongressSummary;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaCallCongressAdapter implements CallCongressPort {

    private final EntityManager entityManager;

    public JpaCallCongressAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<CallCongressSummary> findManageableCongressById(UUID congressId) {
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
        return Optional.of(CallCongressSummary.builder()
                .congressId((UUID) row[0])
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
