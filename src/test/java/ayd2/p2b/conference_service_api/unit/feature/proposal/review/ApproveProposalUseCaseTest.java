package ayd2.p2b.conference_service_api.unit.feature.proposal.review;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalCallPort;
import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalCommitteeMembershipPort;
import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalRepositoryPort;
import ayd2.p2b.conference_service_api.feature.proposal.application.review.ApproveProposalUseCase;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.Proposal;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalStatus;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalType;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalCallStatus;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalCallSummary;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalRequesterContext;
import ayd2.p2b.conference_service_api.feature.proposal.dto.response.ProposalResponse;
import ayd2.p2b.conference_service_api.feature.proposal.mapper.ProposalMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApproveProposalUseCaseTest {

    @Mock
    private ProposalRepositoryPort proposalRepositoryPort;
    @Mock
    private ProposalCallPort proposalCallPort;
    @Mock
    private ProposalCommitteeMembershipPort proposalCommitteeMembershipPort;
    @Mock
    private ProposalMapper proposalMapper;

    private ApproveProposalUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ApproveProposalUseCase(
                proposalRepositoryPort,
                proposalCallPort,
                proposalCommitteeMembershipPort,
                proposalMapper
        );
    }

    @Test
    void shouldApprovePendingProposalForCommitteeMember() {
        UUID proposalId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        Proposal pending = proposal(proposalId, ProposalStatus.PENDING);

        when(proposalRepositoryPort.findById(proposalId)).thenReturn(Optional.of(pending));
        when(proposalCallPort.findVisibleCallById(pending.getCallId())).thenReturn(Optional.of(callSummary(pending.getCallId())));
        when(proposalCommitteeMembershipPort.existsByCongressIdAndUserId(any(), any())).thenReturn(true);
        when(proposalRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(proposalMapper.toResponse(any())).thenAnswer(invocation -> {
            Proposal proposal = invocation.getArgument(0);
            return ProposalResponse.builder()
                    .id(proposal.getId())
                    .callId(proposal.getCallId())
                    .authorUserId(proposal.getAuthorUserId())
                    .title(proposal.getTitle())
                    .description(proposal.getDescription())
                    .type(proposal.getType())
                    .status(proposal.getStatus())
                    .reviewedBy(proposal.getReviewedBy())
                    .reviewedAt(proposal.getReviewedAt())
                    .createdAt(proposal.getCreatedAt())
                    .build();
        });

        ProposalResponse response = useCase.execute(proposalId, requester(reviewerId));

        ArgumentCaptor<Proposal> saveCaptor = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepositoryPort).save(saveCaptor.capture());
        Proposal saved = saveCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ProposalStatus.APPROVED);
        assertThat(saved.getReviewedBy()).isEqualTo(reviewerId);
        assertThat(saved.getReviewedAt()).isNotNull();
        assertThat(saved.getCreatedActivityId()).isNull();

        assertThat(response.getStatus()).isEqualTo(ProposalStatus.APPROVED);
        assertThat(response.getReviewedBy()).isEqualTo(reviewerId);
    }

    @Test
    void shouldRejectApproveWhenRequesterIsNotCommitteeMember() {
        UUID proposalId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        Proposal pending = proposal(proposalId, ProposalStatus.PENDING);

        when(proposalRepositoryPort.findById(proposalId)).thenReturn(Optional.of(pending));
        when(proposalCallPort.findVisibleCallById(pending.getCallId())).thenReturn(Optional.of(callSummary(pending.getCallId())));
        when(proposalCommitteeMembershipPort.existsByCongressIdAndUserId(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(proposalId, requester(reviewerId)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
                });

        verify(proposalRepositoryPort, never()).save(any());
    }

    @Test
    void shouldRejectApproveWhenProposalIsAlreadyReviewed() {
        UUID reviewerId = UUID.randomUUID();
        assertConflictForStatus(ProposalStatus.REJECTED, reviewerId);
        assertConflictForStatus(ProposalStatus.APPROVED, reviewerId);
    }

    private void assertConflictForStatus(ProposalStatus status, UUID reviewerId) {
        UUID proposalId = UUID.randomUUID();
        Proposal reviewed = proposal(proposalId, status).toBuilder()
                .reviewedBy(UUID.randomUUID())
                .reviewedAt(OffsetDateTime.now().minusDays(1))
                .build();

        when(proposalRepositoryPort.findById(proposalId)).thenReturn(Optional.of(reviewed));
        when(proposalCallPort.findVisibleCallById(reviewed.getCallId())).thenReturn(Optional.of(callSummary(reviewed.getCallId())));
        when(proposalCommitteeMembershipPort.existsByCongressIdAndUserId(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(proposalId, requester(reviewerId)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("resource.conflict");
                });
    }

    private ProposalRequesterContext requester(UUID userId) {
        return ProposalRequesterContext.builder()
                .userId(userId)
                .roles(Set.of(Role.PARTICIPANT))
                .accessToken("token")
                .build();
    }

    private ProposalCallSummary callSummary(UUID callId) {
        return ProposalCallSummary.builder()
                .callId(callId)
                .congressId(UUID.randomUUID())
                .institutionId(UUID.randomUUID())
                .congressCreatedBy(UUID.randomUUID())
                .status(ProposalCallStatus.OPEN)
                .build();
    }

    private Proposal proposal(UUID proposalId, ProposalStatus status) {
        UUID authorId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        return Proposal.builder()
                .id(proposalId)
                .callId(UUID.randomUUID())
                .authorUserId(authorId)
                .title("Idempotent event handling")
                .description("Patterns for idempotent message consumers.")
                .type(ProposalType.PONENCIA)
                .status(status)
                .createdBy(authorId)
                .createdAt(now.minusDays(1))
                .updatedAt(now.minusDays(1))
                .build();
    }
}
