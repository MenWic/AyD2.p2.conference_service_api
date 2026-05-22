package ayd2.p2b.conference_service_api.feature.committee.infrastructure.persistence.repository;

import ayd2.p2b.conference_service_api.feature.committee.infrastructure.persistence.entity.CommitteeMemberEntity;
import ayd2.p2b.conference_service_api.feature.committee.infrastructure.persistence.entity.CommitteeMemberId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommitteeMemberRepository extends JpaRepository<CommitteeMemberEntity, CommitteeMemberId> {
    Page<CommitteeMemberEntity> findByCongressId(UUID congressId, Pageable pageable);

    boolean existsByCongressIdAndUserId(UUID congressId, UUID userId);

    void deleteByCongressIdAndUserId(UUID congressId, UUID userId);
}
