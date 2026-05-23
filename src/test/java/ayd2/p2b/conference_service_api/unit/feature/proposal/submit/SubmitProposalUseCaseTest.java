package ayd2.p2b.conference_service_api.unit.feature.proposal.submit;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalCallPort;
import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalRepositoryPort;
import ayd2.p2b.conference_service_api.feature.proposal.application.submit.SubmitProposalUseCase;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.Proposal;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalStatus;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalType;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalCallStatus;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalCallSummary;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalRequesterContext;
import ayd2.p2b.conference_service_api.feature.proposal.dto.request.CreateProposalRequest;
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
class SubmitProposalUseCaseTest {

    @Mock
    private ProposalRepositoryPort proposalRepositoryPort;
    @Mock
    private ProposalCallPort proposalCallPort;
    @Mock
    private ProposalMapper proposalMapper;

    private SubmitProposalUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SubmitProposalUseCase(proposalRepositoryPort, proposalCallPort, proposalMapper);
    }

    @Test
    void shouldCreatePendingProposalForParticipantInOpenCall() {
        UUID callId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CreateProposalRequest request = validRequest();
        Proposal saved = savedProposal(callId, userId, ProposalStatus.PENDING);

        when(proposalCallPort.findVisibleCallById(callId)).thenReturn(java.util.Optional.of(callSummary(callId, ProposalCallStatus.OPEN)));
        when(proposalRepositoryPort.save(any())).thenReturn(saved);
        when(proposalMapper.toResponse(saved)).thenReturn(responseFrom(saved));

        ProposalResponse response = useCase.execute(callId, request, requester(userId, Set.of(Role.PARTICIPANT)));

        ArgumentCaptor<Proposal> captor = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepositoryPort).save(captor.capture());
        Proposal persisted = captor.getValue();
        assertThat(persisted.getAuthorUserId()).isEqualTo(userId);
        assertThat(persisted.getCreatedBy()).isEqualTo(userId);
        assertThat(persisted.getStatus()).isEqualTo(ProposalStatus.PENDING);
        assertThat(persisted.getReviewedBy()).isNull();
        assertThat(persisted.getReviewedAt()).isNull();

        assertThat(response.getId()).isEqualTo(saved.getId());
        assertThat(response.getStatus()).isEqualTo(ProposalStatus.PENDING);
        assertThat(response.getAuthorUserId()).isEqualTo(userId);
    }

    @Test
    void shouldRejectInvalidCreateRequest() {
        UUID callId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProposalRequesterContext requester = requester(userId, Set.of(Role.PARTICIPANT));

        assertValidationFailed(() -> useCase.execute(callId, CreateProposalRequest.builder()
                .title(" ")
                .description("Valid description")
                .type(ProposalType.PONENCIA)
                .build(), requester));

        assertValidationFailed(() -> useCase.execute(callId, CreateProposalRequest.builder()
                .title("Valid title")
                .description("  ")
                .type(ProposalType.TALLER)
                .build(), requester));

        assertValidationFailed(() -> useCase.execute(callId, CreateProposalRequest.builder()
                .title("Valid title")
                .description("Valid description")
                .type(null)
                .build(), requester));

        verify(proposalCallPort, never()).findVisibleCallById(any());
        verify(proposalRepositoryPort, never()).save(any());
    }

    @Test
    void shouldRejectSubmitWhenRequesterIsNotParticipant() {
        UUID callId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.execute(callId, validRequest(), requester(userId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
                });

        verify(proposalCallPort, never()).findVisibleCallById(any());
    }

    @Test
    void shouldRejectSubmitWhenCallIsClosed() {
        UUID callId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(proposalCallPort.findVisibleCallById(callId)).thenReturn(java.util.Optional.of(callSummary(callId, ProposalCallStatus.CLOSED)));

        assertThatThrownBy(() -> useCase.execute(callId, validRequest(), requester(userId, Set.of(Role.PARTICIPANT))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(apiException.getCode()).isEqualTo("domain.invariant_violated");
                });

        verify(proposalRepositoryPort, never()).save(any());
    }

    @Test
    void shouldAllowNewSubmissionEvenWhenThereArePreviousRejectedProposals() {
        UUID callId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Proposal saved = savedProposal(callId, userId, ProposalStatus.PENDING);

        when(proposalCallPort.findVisibleCallById(callId)).thenReturn(java.util.Optional.of(callSummary(callId, ProposalCallStatus.OPEN)));
        when(proposalRepositoryPort.save(any())).thenReturn(saved);
        when(proposalMapper.toResponse(saved)).thenReturn(responseFrom(saved));

        ProposalResponse response = useCase.execute(callId, validRequest(), requester(userId, Set.of(Role.PARTICIPANT)));

        verify(proposalRepositoryPort).save(any());
        assertThat(response.getStatus()).isEqualTo(ProposalStatus.PENDING);
    }

    private void assertValidationFailed(org.assertj.core.api.ThrowableAssert.ThrowingCallable executable) {
        assertThatThrownBy(executable)
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiException.getCode()).isEqualTo("validation.failed");
                });
    }

    private CreateProposalRequest validRequest() {
        return CreateProposalRequest.builder()
                .title("Reactive streams in distributed systems")
                .description("A practical study on resilience patterns.")
                .type(ProposalType.PONENCIA)
                .build();
    }

    private ProposalRequesterContext requester(UUID userId, Set<Role> roles) {
        return ProposalRequesterContext.builder()
                .userId(userId)
                .roles(roles)
                .accessToken("token")
                .build();
    }

    private ProposalCallSummary callSummary(UUID callId, ProposalCallStatus status) {
        return ProposalCallSummary.builder()
                .callId(callId)
                .congressId(UUID.randomUUID())
                .institutionId(UUID.randomUUID())
                .congressCreatedBy(UUID.randomUUID())
                .status(status)
                .build();
    }

    private Proposal savedProposal(UUID callId, UUID authorUserId, ProposalStatus status) {
        OffsetDateTime now = OffsetDateTime.now();
        return Proposal.builder()
                .id(UUID.randomUUID())
                .callId(callId)
                .authorUserId(authorUserId)
                .title("Reactive streams in distributed systems")
                .description("A practical study on resilience patterns.")
                .type(ProposalType.PONENCIA)
                .status(status)
                .createdBy(authorUserId)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private ProposalResponse responseFrom(Proposal proposal) {
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
    }
}
