package ayd2.p2b.conference_service_api.feature.committee.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.committee.application.port.CommitteeCongressPort;
import ayd2.p2b.conference_service_api.feature.committee.dto.internal.CommitteeCongressSummary;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaCommitteeCongressAdapter implements CommitteeCongressPort {

    private final EntityManager entityManager;

    public JpaCommitteeCongressAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<CommitteeCongressSummary> findManageableCongressById(UUID congressId) {
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
        return Optional.of(CommitteeCongressSummary.builder()
                .congressId((UUID) row[0])
                .institutionId((UUID) row[1])
                .createdBy((UUID) row[2])
                .build());
    }
}
