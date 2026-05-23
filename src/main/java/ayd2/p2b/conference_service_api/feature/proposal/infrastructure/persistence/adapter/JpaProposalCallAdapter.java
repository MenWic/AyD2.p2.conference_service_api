package ayd2.p2b.conference_service_api.feature.proposal.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalCallPort;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalCallStatus;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalCallSummary;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaProposalCallAdapter implements ProposalCallPort {

    private final EntityManager entityManager;

    public JpaProposalCallAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<ProposalCallSummary> findVisibleCallById(UUID callId) {
        Query query = entityManager.createNativeQuery("""
                select c.id, c.congress_id, cg.institution_id, cg.created_by, c.status
                from calls c
                join congresses cg on cg.id = c.congress_id
                join institutions i on i.id = cg.institution_id
                where c.id = :callId and i.active = true
                """);
        query.setParameter("callId", callId);

        @SuppressWarnings("unchecked")
        var rows = query.getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = (Object[]) rows.getFirst();
        String status = row[4] == null ? null : row[4].toString();

        return Optional.of(ProposalCallSummary.builder()
                .callId((UUID) row[0])
                .congressId((UUID) row[1])
                .institutionId((UUID) row[2])
                .congressCreatedBy((UUID) row[3])
                .status(status == null ? null : ProposalCallStatus.valueOf(status))
                .build());
    }
}
