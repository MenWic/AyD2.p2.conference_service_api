package ayd2.p2b.conference_service_api.unit.feature.proposal;

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
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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

    private Proposal buildProposal(UUID callId, UUID authorId) {
        return Proposal.builder()
                .id(UUID.randomUUID()).callId(callId).authorUserId(authorId)
                .title("Test Proposal").description("Desc")
                .type(ProposalType.PONENCIA).status(ProposalStatus.PENDING)
                .createdBy(authorId)
                .build();
    }

    @Test
    void save_delegates_to_repository_and_mapper() {
        UUID callId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Proposal proposal = buildProposal(callId, authorId);
        ProposalEntity entity = new ProposalEntity();
        ProposalEntity saved = new ProposalEntity();
        given(proposalMapper.toEntity(proposal)).willReturn(entity);
        given(proposalRepository.save(entity)).willReturn(saved);
        given(proposalMapper.toDomain(saved)).willReturn(proposal);

        Proposal result = adapter.save(proposal);

        assertThat(result).isEqualTo(proposal);
        verify(proposalRepository).save(entity);
    }

    @Test
    void findById_returns_mapped_when_present() {
        UUID proposalId = UUID.randomUUID();
        ProposalEntity entity = new ProposalEntity();
        Proposal expected = buildProposal(UUID.randomUUID(), UUID.randomUUID());
        given(proposalRepository.findById(proposalId)).willReturn(Optional.of(entity));
        given(proposalMapper.toDomain(entity)).willReturn(expected);

        Optional<Proposal> result = adapter.findById(proposalId);

        assertThat(result).isPresent().contains(expected);
    }

    @Test
    void findById_returns_empty_when_not_found() {
        UUID proposalId = UUID.randomUUID();
        given(proposalRepository.findById(proposalId)).willReturn(Optional.empty());

        Optional<Proposal> result = adapter.findById(proposalId);

        assertThat(result).isEmpty();
    }

    @Test
    void findByCallId_returns_mapped_page() {
        UUID callId = UUID.randomUUID();
        Pageable pageable = Pageable.unpaged();
        ProposalEntity entity = new ProposalEntity();
        Proposal proposal = buildProposal(callId, UUID.randomUUID());
        given(proposalRepository.findByCallId(callId, pageable))
                .willReturn(new PageImpl<>(List.of(entity)));
        given(proposalMapper.toDomain(entity)).willReturn(proposal);

        Page<Proposal> result = adapter.findByCallId(callId, pageable);

        assertThat(result.getContent()).containsExactly(proposal);
    }

    @Test
    void findByAuthorUserId_returns_mapped_page() {
        UUID authorId = UUID.randomUUID();
        Pageable pageable = Pageable.unpaged();
        ProposalEntity entity = new ProposalEntity();
        Proposal proposal = buildProposal(UUID.randomUUID(), authorId);
        given(proposalRepository.findByAuthorUserId(authorId, pageable))
                .willReturn(new PageImpl<>(List.of(entity)));
        given(proposalMapper.toDomain(entity)).willReturn(proposal);

        Page<Proposal> result = adapter.findByAuthorUserId(authorId, pageable);

        assertThat(result.getContent()).containsExactly(proposal);
    }
}
