package ayd2.p2b.conference_service_api.unit.feature.proposal.list;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.proposal.application.list.ListProposalsByCallUseCase;
import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalCallPort;
import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalCommitteeMembershipPort;
import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalRepositoryPort;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.Proposal;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalStatus;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalType;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalCallStatus;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalCallSummary;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalRequesterContext;
import ayd2.p2b.conference_service_api.feature.proposal.dto.response.ProposalResponse;
import ayd2.p2b.conference_service_api.feature.proposal.mapper.ProposalMapper;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListProposalsByCallUseCaseTest {

    @Mock
    private ProposalRepositoryPort proposalRepositoryPort;
    @Mock
    private ProposalCallPort proposalCallPort;
    @Mock
    private ProposalCommitteeMembershipPort proposalCommitteeMembershipPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;
    @Mock
    private ProposalMapper proposalMapper;

    private ListProposalsByCallUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListProposalsByCallUseCase(
                proposalRepositoryPort,
                proposalCallPort,
                proposalCommitteeMembershipPort,
                iamUserLookupPort,
                proposalMapper
        );
    }

    @Test
    void shouldAllowScopedCongressAdminToListByCall() {
        UUID callId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        ProposalCallSummary callSummary = callSummary(callId, UUID.randomUUID());
        Proposal proposal = proposal(callId, UUID.randomUUID());
        PageRequest pageable = PageRequest.of(0, 20);

        when(proposalCallPort.findVisibleCallById(callId)).thenReturn(Optional.of(callSummary));
        when(proposalCommitteeMembershipPort.existsByCongressIdAndUserId(callSummary.getCongressId(), requesterId)).thenReturn(false);
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(
                requesterId,
                callSummary.getInstitutionId(),
                "token"
        )).thenReturn(true);
        when(proposalRepositoryPort.findByCallId(callId, pageable))
                .thenReturn(new PageImpl<>(List.of(proposal), pageable, 1));
        when(proposalMapper.toResponse(proposal)).thenReturn(response(proposal));

        PageResponse<ProposalResponse> pageResponse = useCase.execute(
                callId,
                pageable,
                requester(requesterId, Set.of(Role.CONGRESS_ADMIN))
        );

        assertThat(pageResponse.getItems()).hasSize(1);
        assertThat(pageResponse.getItems().getFirst().getId()).isEqualTo(proposal.getId());
    }

    @Test
    void shouldAllowCommitteeMemberForCallCongressToList() {
        UUID callId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        ProposalCallSummary callSummary = callSummary(callId, UUID.randomUUID());
        Proposal proposal = proposal(callId, UUID.randomUUID());
        PageRequest pageable = PageRequest.of(0, 20);

        when(proposalCallPort.findVisibleCallById(callId)).thenReturn(Optional.of(callSummary));
        when(proposalCommitteeMembershipPort.existsByCongressIdAndUserId(callSummary.getCongressId(), requesterId)).thenReturn(true);
        when(proposalRepositoryPort.findByCallId(callId, pageable))
                .thenReturn(new PageImpl<>(List.of(proposal), pageable, 1));
        when(proposalMapper.toResponse(proposal)).thenReturn(response(proposal));

        PageResponse<ProposalResponse> pageResponse = useCase.execute(
                callId,
                pageable,
                requester(requesterId, Set.of(Role.PARTICIPANT))
        );

        verify(iamUserLookupPort, never()).isCongressAdminLinkedToInstitution(any(), any(), any());
        assertThat(pageResponse.getItems()).hasSize(1);
    }

    @Test
    void shouldRejectCommitteeMemberFromDifferentCongress() {
        UUID callId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        ProposalCallSummary callSummary = callSummary(callId, UUID.randomUUID());

        when(proposalCallPort.findVisibleCallById(callId)).thenReturn(Optional.of(callSummary));
        when(proposalCommitteeMembershipPort.existsByCongressIdAndUserId(callSummary.getCongressId(), requesterId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(callId, PageRequest.of(0, 20), requester(requesterId, Set.of(Role.PARTICIPANT))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
                });

        verify(proposalRepositoryPort, never()).findByCallId(any(), any());
    }

    @Test
    void shouldRejectUnrelatedAuthenticatedUser() {
        UUID callId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        ProposalCallSummary callSummary = callSummary(callId, UUID.randomUUID());

        when(proposalCallPort.findVisibleCallById(callId)).thenReturn(Optional.of(callSummary));
        when(proposalCommitteeMembershipPort.existsByCongressIdAndUserId(callSummary.getCongressId(), requesterId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(callId, PageRequest.of(0, 20), requester(requesterId, Set.of(Role.GUEST_SPEAKER))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
                });

        verify(proposalRepositoryPort, never()).findByCallId(any(), any());
    }

    private ProposalRequesterContext requester(UUID userId, Set<Role> roles) {
        return ProposalRequesterContext.builder()
                .userId(userId)
                .roles(roles)
                .accessToken("token")
                .build();
    }

    private ProposalCallSummary callSummary(UUID callId, UUID congressCreatedBy) {
        return ProposalCallSummary.builder()
                .callId(callId)
                .congressId(UUID.randomUUID())
                .institutionId(UUID.randomUUID())
                .congressCreatedBy(congressCreatedBy)
                .status(ProposalCallStatus.OPEN)
                .build();
    }

    private Proposal proposal(UUID callId, UUID authorUserId) {
        OffsetDateTime now = OffsetDateTime.now();
        return Proposal.builder()
                .id(UUID.randomUUID())
                .callId(callId)
                .authorUserId(authorUserId)
                .title("Event-driven design")
                .description("Designing event-driven systems for conferences.")
                .type(ProposalType.PONENCIA)
                .status(ProposalStatus.PENDING)
                .createdBy(authorUserId)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private ProposalResponse response(Proposal proposal) {
        return ProposalResponse.builder()
                .id(proposal.getId())
                .callId(proposal.getCallId())
                .authorUserId(proposal.getAuthorUserId())
                .title(proposal.getTitle())
                .description(proposal.getDescription())
                .type(proposal.getType())
                .status(proposal.getStatus())
                .createdAt(proposal.getCreatedAt())
                .build();
    }
}
