package ayd2.p2b.conference_service_api.unit.feature.proposal.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.proposal.domain.model.Proposal;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalStatus;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalType;
import ayd2.p2b.conference_service_api.feature.proposal.infrastructure.persistence.adapter.JpaProposalRepositoryAdapter;
import ayd2.p2b.conference_service_api.feature.proposal.infrastructure.persistence.entity.ProposalEntity;
import ayd2.p2b.conference_service_api.feature.proposal.infrastructure.persistence.repository.ProposalRepository;
import ayd2.p2b.conference_service_api.feature.proposal.mapper.ProposalMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaProposalRepositoryAdapterTest {

    @Mock
    private ProposalRepository proposalRepository;
    @Mock
    private ProposalMapper proposalMapper;

    private JpaProposalRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaProposalRepositoryAdapter(proposalRepository, proposalMapper);
    }

    @Test
    void shouldSaveProposalThroughRepositoryAndMapper() {
        Proposal proposal = sampleProposal();
        ProposalEntity entity = new ProposalEntity();
        ProposalEntity savedEntity = new ProposalEntity();
        savedEntity.setId(proposal.getId());

        when(proposalMapper.toEntity(proposal)).thenReturn(entity);
        when(proposalRepository.save(entity)).thenReturn(savedEntity);
        when(proposalMapper.toDomain(savedEntity)).thenReturn(proposal);

        Proposal result = adapter.save(proposal);

        assertThat(result).isEqualTo(proposal);
    }

    @Test
    void shouldFindByIdWhenPresent() {
        UUID proposalId = UUID.randomUUID();
        ProposalEntity entity = new ProposalEntity();
        Proposal proposal = sampleProposal();

        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(entity));
        when(proposalMapper.toDomain(entity)).thenReturn(proposal);

        assertThat(adapter.findById(proposalId)).contains(proposal);
    }

    @Test
    void shouldReturnEmptyWhenFindByIdIsMissing() {
        UUID proposalId = UUID.randomUUID();
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.empty());

        assertThat(adapter.findById(proposalId)).isEmpty();
    }

    @Test
    void shouldFindByCallIdAndMapPageContent() {
        UUID callId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 1);
        ProposalEntity entity = new ProposalEntity();
        entity.setId(UUID.randomUUID());
        Proposal proposal = sampleProposal().toBuilder().id(entity.getId()).build();

        when(proposalRepository.findByCallId(callId, pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 4));
        when(proposalMapper.toDomain(entity)).thenReturn(proposal);

        Page<Proposal> result = adapter.findByCallId(callId, pageable);

        assertThat(result.getContent()).containsExactly(proposal);
        assertThat(result.getTotalElements()).isEqualTo(4);
    }

    @Test
    void shouldFindByAuthorUserIdAndMapPageContent() {
        UUID authorUserId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 1);
        ProposalEntity entity = new ProposalEntity();
        entity.setId(UUID.randomUUID());
        Proposal proposal = sampleProposal().toBuilder().id(entity.getId()).build();

        when(proposalRepository.findByAuthorUserId(authorUserId, pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 2));
        when(proposalMapper.toDomain(entity)).thenReturn(proposal);

        Page<Proposal> result = adapter.findByAuthorUserId(authorUserId, pageable);

        assertThat(result.getContent()).containsExactly(proposal);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    private Proposal sampleProposal() {
        return Proposal.builder()
                .id(UUID.randomUUID())
                .callId(UUID.randomUUID())
                .authorUserId(UUID.randomUUID())
                .title("Proposal")
                .description("Description")
                .type(ProposalType.PONENCIA)
                .status(ProposalStatus.PENDING)
                .createdBy(UUID.randomUUID())
                .build();
    }
}
