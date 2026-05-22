package ayd2.p2b.conference_service_api.unit.feature.committee.list;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.committee.application.list.ListCommitteeMembersUseCase;
import ayd2.p2b.conference_service_api.feature.committee.application.port.CommitteeCongressPort;
import ayd2.p2b.conference_service_api.feature.committee.application.port.CommitteeRepositoryPort;
import ayd2.p2b.conference_service_api.feature.committee.domain.model.CommitteeMember;
import ayd2.p2b.conference_service_api.feature.committee.dto.internal.CommitteeCongressSummary;
import ayd2.p2b.conference_service_api.feature.committee.dto.internal.CommitteeRequesterContext;
import ayd2.p2b.conference_service_api.feature.committee.dto.response.CommitteeMemberResponse;
import ayd2.p2b.conference_service_api.feature.committee.mapper.CommitteeMapper;
import ayd2.p2b.conference_service_api.integration.dto.IamUserSummary;
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
import java.util.Map;
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
class ListCommitteeMembersUseCaseTest {

    @Mock
    private CommitteeRepositoryPort committeeRepositoryPort;
    @Mock
    private CommitteeCongressPort committeeCongressPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;
    @Mock
    private CommitteeMapper committeeMapper;

    private ListCommitteeMembersUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListCommitteeMembersUseCase(
                committeeRepositoryPort,
                committeeCongressPort,
                iamUserLookupPort,
                committeeMapper
        );
    }

    @Test
    void shouldAllowSystemAdminToListMembers() {
        UUID congressId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CommitteeMember member = CommitteeMember.builder()
                .congressId(congressId)
                .userId(userId)
                .addedAt(OffsetDateTime.now())
                .build();
        CommitteeCongressSummary congress = congressSummary(congressId, UUID.randomUUID());
        PageRequest pageable = PageRequest.of(0, 20);
        IamUserSummary summary = userSummary(userId);

        when(committeeCongressPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congress));
        when(committeeRepositoryPort.findByCongressId(congressId, pageable))
                .thenReturn(new PageImpl<>(List.of(member), pageable, 1));
        when(iamUserLookupPort.getUsersSummary(Set.of(userId), "token"))
                .thenReturn(Map.of(userId, summary));
        when(committeeMapper.toResponse(member, summary)).thenReturn(CommitteeMemberResponse.builder()
                .congressId(congressId)
                .userId(userId)
                .fullName(summary.getFullName())
                .email(summary.getEmail())
                .addedAt(member.getAddedAt())
                .build());

        PageResponse<CommitteeMemberResponse> response = useCase.execute(
                congressId,
                pageable,
                requester(UUID.randomUUID(), Set.of(Role.SYSTEM_ADMIN, Role.PARTICIPANT))
        );

        verify(iamUserLookupPort, never()).isCongressAdminLinkedToInstitution(any(), any(), any());
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().getFirst().getFullName()).isEqualTo("Grace Hopper");
    }

    @Test
    void shouldRejectUnrelatedCongressAdmin() {
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        CommitteeCongressSummary congress = congressSummary(congressId, ownerId);

        when(committeeCongressPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congress));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(
                eq(actorId),
                eq(congress.getInstitutionId()),
                eq("token")
        )).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(
                congressId,
                PageRequest.of(0, 20),
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
        UUID userId = UUID.randomUUID();
        CommitteeMember member = CommitteeMember.builder()
                .congressId(congressId)
                .userId(userId)
                .addedAt(OffsetDateTime.now())
                .build();
        PageRequest pageable = PageRequest.of(0, 20);

        when(committeeCongressPort.findManageableCongressById(congressId))
                .thenReturn(Optional.of(congressSummary(congressId, ownerId)));
        when(committeeRepositoryPort.findByCongressId(congressId, pageable))
                .thenReturn(new PageImpl<>(List.of(member), pageable, 1));
        when(iamUserLookupPort.getUsersSummary(Set.of(userId), "token"))
                .thenReturn(Map.of());

        assertThatThrownBy(() -> useCase.execute(
                congressId,
                pageable,
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

    private IamUserSummary userSummary(UUID userId) {
        return IamUserSummary.builder()
                .id(userId)
                .active(true)
                .roles(Set.of("PARTICIPANT"))
                .fullName("Grace Hopper")
                .email("grace@example.com")
                .build();
    }
}
