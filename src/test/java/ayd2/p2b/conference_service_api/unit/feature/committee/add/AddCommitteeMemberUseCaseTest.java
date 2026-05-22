package ayd2.p2b.conference_service_api.unit.feature.committee.add;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.committee.application.add.AddCommitteeMemberUseCase;
import ayd2.p2b.conference_service_api.feature.committee.application.port.CommitteeCongressPort;
import ayd2.p2b.conference_service_api.feature.committee.application.port.CommitteeRepositoryPort;
import ayd2.p2b.conference_service_api.feature.committee.domain.model.CommitteeMember;
import ayd2.p2b.conference_service_api.feature.committee.dto.internal.CommitteeCongressSummary;
import ayd2.p2b.conference_service_api.feature.committee.dto.internal.CommitteeRequesterContext;
import ayd2.p2b.conference_service_api.feature.committee.dto.request.AddCommitteeMemberRequest;
import ayd2.p2b.conference_service_api.feature.committee.dto.response.CommitteeMemberResponse;
import ayd2.p2b.conference_service_api.feature.committee.mapper.CommitteeMapper;
import ayd2.p2b.conference_service_api.integration.dto.IamCommitteeCandidateSummary;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddCommitteeMemberUseCaseTest {

    @Mock
    private CommitteeRepositoryPort committeeRepositoryPort;
    @Mock
    private CommitteeCongressPort committeeCongressPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;
    @Mock
    private CommitteeMapper committeeMapper;

    private AddCommitteeMemberUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AddCommitteeMemberUseCase(
                committeeRepositoryPort,
                committeeCongressPort,
                iamUserLookupPort,
                committeeMapper
        );
    }

    @Test
    void shouldAddEligibleUserAsCommitteeMember() {
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        CommitteeCongressSummary congress = congressSummary(congressId, ownerId);
        IamCommitteeCandidateSummary summary = candidateSummary(candidateId);
        CommitteeMember saved = CommitteeMember.builder()
                .congressId(congressId)
                .userId(candidateId)
                .addedAt(OffsetDateTime.now())
                .addedBy(ownerId)
                .build();

        when(committeeCongressPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congress));
        when(committeeRepositoryPort.existsByCongressIdAndUserId(congressId, candidateId)).thenReturn(false);
        when(iamUserLookupPort.getCommitteeCandidateSummary(candidateId, "token")).thenReturn(summary);
        when(committeeRepositoryPort.save(any())).thenReturn(saved);
        when(committeeMapper.toResponse(saved, summary)).thenReturn(CommitteeMemberResponse.builder()
                .congressId(congressId)
                .userId(candidateId)
                .fullName(summary.getFullName())
                .email(summary.getEmail())
                .addedAt(saved.getAddedAt())
                .build());

        CommitteeMemberResponse response = useCase.execute(
                congressId,
                AddCommitteeMemberRequest.builder().userId(candidateId).build(),
                requester(ownerId, Set.of(Role.CONGRESS_ADMIN))
        );

        verify(iamUserLookupPort, never()).isCongressAdminLinkedToInstitution(any(), any(), any());
        verify(iamUserLookupPort, never()).getUsersSummary(any(), any());
        assertThat(response.getUserId()).isEqualTo(candidateId);
        assertThat(response.getFullName()).isEqualTo("Ada Lovelace");
        assertThat(response.getEmail()).isEqualTo("ada@example.com");
    }

    @Test
    void shouldRejectDuplicateMember() {
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();

        when(committeeCongressPort.findManageableCongressById(congressId))
                .thenReturn(Optional.of(congressSummary(congressId, ownerId)));
        when(committeeRepositoryPort.existsByCongressIdAndUserId(congressId, candidateId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(
                congressId,
                AddCommitteeMemberRequest.builder().userId(candidateId).build(),
                requester(ownerId, Set.of(Role.CONGRESS_ADMIN))
        )).isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("resource.conflict");
                });
    }

    @Test
    void shouldRejectIneligibleUser() {
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();

        when(committeeCongressPort.findManageableCongressById(congressId))
                .thenReturn(Optional.of(congressSummary(congressId, ownerId)));
        when(committeeRepositoryPort.existsByCongressIdAndUserId(congressId, candidateId)).thenReturn(false);
        when(iamUserLookupPort.getCommitteeCandidateSummary(candidateId, "token"))
                .thenReturn(IamCommitteeCandidateSummary.builder()
                        .eligible(false)
                        .userId(candidateId)
                        .build());

        assertThatThrownBy(() -> useCase.execute(
                congressId,
                AddCommitteeMemberRequest.builder().userId(candidateId).build(),
                requester(ownerId, Set.of(Role.CONGRESS_ADMIN))
        )).isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(apiException.getCode()).isEqualTo("domain.invariant_violated");
                });
    }

    @Test
    void shouldRejectUnrelatedCongressAdmin() {
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        CommitteeCongressSummary congress = congressSummary(congressId, ownerId);

        when(committeeCongressPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congress));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(
                eq(actorId),
                eq(congress.getInstitutionId()),
                eq("token")
        )).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(
                congressId,
                AddCommitteeMemberRequest.builder().userId(candidateId).build(),
                requester(actorId, Set.of(Role.CONGRESS_ADMIN))
        )).isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
                });
    }

    @Test
    void shouldReturnControlledErrorWhenIamProfileSummaryIsMissing() {
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();

        when(committeeCongressPort.findManageableCongressById(congressId))
                .thenReturn(Optional.of(congressSummary(congressId, ownerId)));
        when(committeeRepositoryPort.existsByCongressIdAndUserId(congressId, candidateId)).thenReturn(false);
        when(iamUserLookupPort.getCommitteeCandidateSummary(candidateId, "token"))
                .thenReturn(IamCommitteeCandidateSummary.builder()
                        .eligible(true)
                        .userId(candidateId)
                        .fullName(null)
                        .email("ada@example.com")
                        .build());

        assertThatThrownBy(() -> useCase.execute(
                congressId,
                AddCommitteeMemberRequest.builder().userId(candidateId).build(),
                requester(ownerId, Set.of(Role.CONGRESS_ADMIN))
        )).isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
    }

    private CommitteeRequesterContext requester(UUID userId, Set<Role> roles) {
        return CommitteeRequesterContext.builder()
                .userId(userId)
                .roles(roles)
                .accessToken("token")
                .build();
    }

    private CommitteeCongressSummary congressSummary(UUID congressId, UUID ownerId) {
        return CommitteeCongressSummary.builder()
                .congressId(congressId)
                .institutionId(UUID.randomUUID())
                .createdBy(ownerId)
                .build();
    }

    private IamCommitteeCandidateSummary candidateSummary(UUID userId) {
        return IamCommitteeCandidateSummary.builder()
                .eligible(true)
                .userId(userId)
                .fullName("Ada Lovelace")
                .email("ada@example.com")
                .build();
    }
}
