package ayd2.p2b.conference_service_api.feature.proposal.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalCommitteeMembershipPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JpaProposalCommitteeMembershipAdapter implements ProposalCommitteeMembershipPort {

    private final EntityManager entityManager;

    public JpaProposalCommitteeMembershipAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsByCongressIdAndUserId(UUID congressId, UUID userId) {
        Query query = entityManager.createNativeQuery("""
                select exists(
                    select 1
                    from committee_members cm
                    where cm.congress_id = :congressId and cm.user_id = :userId
                )
                """);
        query.setParameter("congressId", congressId);
        query.setParameter("userId", userId);
        Object value = query.getSingleResult();
        return Boolean.TRUE.equals(value);
    }
}
