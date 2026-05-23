package ayd2.p2b.conference_service_api.feature.proposal.infrastructure.persistence.repository;

import ayd2.p2b.conference_service_api.feature.proposal.infrastructure.persistence.entity.ProposalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProposalRepository extends JpaRepository<ProposalEntity, UUID> {
    Page<ProposalEntity> findByCallId(UUID callId, Pageable pageable);

    Page<ProposalEntity> findByAuthorUserId(UUID authorUserId, Pageable pageable);
}
