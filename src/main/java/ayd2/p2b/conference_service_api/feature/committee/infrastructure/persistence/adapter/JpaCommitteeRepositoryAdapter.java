package ayd2.p2b.conference_service_api.feature.committee.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.committee.application.port.CommitteeRepositoryPort;
import ayd2.p2b.conference_service_api.feature.committee.domain.model.CommitteeMember;
import ayd2.p2b.conference_service_api.feature.committee.infrastructure.persistence.repository.CommitteeMemberRepository;
import ayd2.p2b.conference_service_api.feature.committee.mapper.CommitteeMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JpaCommitteeRepositoryAdapter implements CommitteeRepositoryPort {

    private final CommitteeMemberRepository committeeMemberRepository;
    private final CommitteeMapper committeeMapper;

    public JpaCommitteeRepositoryAdapter(
            CommitteeMemberRepository committeeMemberRepository,
            CommitteeMapper committeeMapper
    ) {
        this.committeeMemberRepository = committeeMemberRepository;
        this.committeeMapper = committeeMapper;
    }

    @Override
    public CommitteeMember save(CommitteeMember member) {
        return committeeMapper.toDomain(committeeMemberRepository.save(committeeMapper.toEntity(member)));
    }

    @Override
    public Page<CommitteeMember> findByCongressId(UUID congressId, Pageable pageable) {
        return committeeMemberRepository.findByCongressId(congressId, pageable)
                .map(committeeMapper::toDomain);
    }

    @Override
    public boolean existsByCongressIdAndUserId(UUID congressId, UUID userId) {
        return committeeMemberRepository.existsByCongressIdAndUserId(congressId, userId);
    }

    @Override
    public void deleteByCongressIdAndUserId(UUID congressId, UUID userId) {
        committeeMemberRepository.deleteByCongressIdAndUserId(congressId, userId);
    }
}
