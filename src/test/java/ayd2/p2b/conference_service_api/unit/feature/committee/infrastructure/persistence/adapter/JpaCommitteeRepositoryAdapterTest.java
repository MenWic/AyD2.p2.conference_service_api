package ayd2.p2b.conference_service_api.unit.feature.committee.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.committee.domain.model.CommitteeMember;
import ayd2.p2b.conference_service_api.feature.committee.infrastructure.persistence.adapter.JpaCommitteeRepositoryAdapter;
import ayd2.p2b.conference_service_api.feature.committee.infrastructure.persistence.entity.CommitteeMemberEntity;
import ayd2.p2b.conference_service_api.feature.committee.infrastructure.persistence.repository.CommitteeMemberRepository;
import ayd2.p2b.conference_service_api.feature.committee.mapper.CommitteeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaCommitteeRepositoryAdapterTest {

    @Mock
    private CommitteeMemberRepository committeeMemberRepository;
    @Mock
    private CommitteeMapper committeeMapper;

    private JpaCommitteeRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaCommitteeRepositoryAdapter(committeeMemberRepository, committeeMapper);
    }

    @Test
    void shouldSaveCommitteeMemberThroughRepositoryAndMapper() {
        CommitteeMember member = sampleMember();
        CommitteeMemberEntity entity = new CommitteeMemberEntity();
        CommitteeMemberEntity savedEntity = new CommitteeMemberEntity();
        savedEntity.setCongressId(member.getCongressId());
        savedEntity.setUserId(member.getUserId());

        when(committeeMapper.toEntity(member)).thenReturn(entity);
        when(committeeMemberRepository.save(entity)).thenReturn(savedEntity);
        when(committeeMapper.toDomain(savedEntity)).thenReturn(member);

        CommitteeMember result = adapter.save(member);

        assertThat(result).isEqualTo(member);
    }

    @Test
    void shouldFindByCongressIdAndMapPageContent() {
        UUID congressId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 1);
        CommitteeMemberEntity entity = new CommitteeMemberEntity();
        entity.setCongressId(congressId);
        entity.setUserId(UUID.randomUUID());
        CommitteeMember member = sampleMember().toBuilder()
                .congressId(entity.getCongressId())
                .userId(entity.getUserId())
                .build();

        when(committeeMemberRepository.findByCongressId(congressId, pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 3));
        when(committeeMapper.toDomain(entity)).thenReturn(member);

        Page<CommitteeMember> result = adapter.findByCongressId(congressId, pageable);

        assertThat(result.getContent()).containsExactly(member);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void shouldDelegateExistsByCongressIdAndUserId() {
        UUID congressId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(committeeMemberRepository.existsByCongressIdAndUserId(congressId, userId)).thenReturn(true);

        boolean exists = adapter.existsByCongressIdAndUserId(congressId, userId);

        assertThat(exists).isTrue();
        verify(committeeMemberRepository).existsByCongressIdAndUserId(congressId, userId);
    }

    @Test
    void shouldDelegateDeleteByCongressIdAndUserId() {
        UUID congressId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        adapter.deleteByCongressIdAndUserId(congressId, userId);

        verify(committeeMemberRepository).deleteByCongressIdAndUserId(congressId, userId);
    }

    private CommitteeMember sampleMember() {
        return CommitteeMember.builder()
                .congressId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .addedAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .addedBy(UUID.randomUUID())
                .build();
    }
}
