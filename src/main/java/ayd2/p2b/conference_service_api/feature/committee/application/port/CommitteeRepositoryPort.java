package ayd2.p2b.conference_service_api.feature.committee.application.port;

import ayd2.p2b.conference_service_api.feature.committee.domain.model.CommitteeMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CommitteeRepositoryPort {
    CommitteeMember save(CommitteeMember member);

    Page<CommitteeMember> findByCongressId(UUID congressId, Pageable pageable);

    boolean existsByCongressIdAndUserId(UUID congressId, UUID userId);

    void deleteByCongressIdAndUserId(UUID congressId, UUID userId);
}
