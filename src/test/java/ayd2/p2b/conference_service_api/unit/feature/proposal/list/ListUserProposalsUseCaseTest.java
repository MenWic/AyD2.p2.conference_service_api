package ayd2.p2b.conference_service_api.unit.feature.proposal.list;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.proposal.application.list.ListUserProposalsUseCase;
import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalRepositoryPort;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.Proposal;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalStatus;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalType;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalRequesterContext;
import ayd2.p2b.conference_service_api.feature.proposal.dto.response.ProposalResponse;
import ayd2.p2b.conference_service_api.feature.proposal.mapper.ProposalMapper;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListUserProposalsUseCaseTest {

    @Mock
    private ProposalRepositoryPort proposalRepositoryPort;
    @Mock
    private ProposalMapper proposalMapper;

    private ListUserProposalsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListUserProposalsUseCase(proposalRepositoryPort, proposalMapper);
    }

    @Test
    void shouldAllowSelfParticipantToListOwnProposals() {
        UUID userId = UUID.randomUUID();
        Proposal proposal = proposal(userId);
        PageRequest pageable = PageRequest.of(0, 20);

        when(proposalRepositoryPort.findByAuthorUserId(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(proposal), pageable, 1));
        when(proposalMapper.toResponse(proposal)).thenReturn(response(proposal));

        PageResponse<ProposalResponse> pageResponse = useCase.execute(
                userId,
                pageable,
                requester(userId, Set.of(Role.PARTICIPANT))
        );

        assertThat(pageResponse.getItems()).hasSize(1);
        assertThat(pageResponse.getItems().getFirst().getAuthorUserId()).isEqualTo(userId);
    }

    @Test
    void shouldRejectOtherUserPathBeforeRepositoryLookup() {
        UUID requesterId = UUID.randomUUID();
        UUID pathUserId = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.execute(
                pathUserId,
                PageRequest.of(0, 20),
                requester(requesterId, Set.of(Role.PARTICIPANT))
        )).isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
                });

        verify(proposalRepositoryPort, never()).findByAuthorUserId(any(), any());
    }

    private ProposalRequesterContext requester(UUID userId, Set<Role> roles) {
        return ProposalRequesterContext.builder()
                .userId(userId)
                .roles(roles)
                .accessToken("token")
                .build();
    }

    private Proposal proposal(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now();
        return Proposal.builder()
                .id(UUID.randomUUID())
                .callId(UUID.randomUUID())
                .authorUserId(userId)
                .title("Consistency models")
                .description("A review of consistency models in distributed systems.")
                .type(ProposalType.PONENCIA)
                .status(ProposalStatus.PENDING)
                .createdBy(userId)
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
