package ayd2.p2b.conference_service_api.unit.feature.committee.remove;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.committee.application.port.CommitteeCongressPort;
import ayd2.p2b.conference_service_api.feature.committee.application.port.CommitteeRepositoryPort;
import ayd2.p2b.conference_service_api.feature.committee.application.remove.RemoveCommitteeMemberUseCase;
import ayd2.p2b.conference_service_api.feature.committee.dto.internal.CommitteeCongressSummary;
import ayd2.p2b.conference_service_api.feature.committee.dto.internal.CommitteeRequesterContext;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveCommitteeMemberUseCaseTest {

    @Mock
    private CommitteeRepositoryPort committeeRepositoryPort;
    @Mock
    private CommitteeCongressPort committeeCongressPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;

    private RemoveCommitteeMemberUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RemoveCommitteeMemberUseCase(
                committeeRepositoryPort,
                committeeCongressPort,
                iamUserLookupPort
        );
    }

    @Test
    void shouldRemoveExistingMembership() {
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        when(committeeCongressPort.findManageableCongressById(congressId))
                .thenReturn(Optional.of(congressSummary(congressId, ownerId)));
        when(committeeRepositoryPort.existsByCongressIdAndUserId(congressId, memberId)).thenReturn(true);

        useCase.execute(congressId, memberId, requester(ownerId, Set.of(Role.CONGRESS_ADMIN)));

        verify(committeeRepositoryPort).deleteByCongressIdAndUserId(congressId, memberId);
    }

    @Test
    void shouldRejectMissingMembership() {
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        when(committeeCongressPort.findManageableCongressById(congressId))
                .thenReturn(Optional.of(congressSummary(congressId, ownerId)));
        when(committeeRepositoryPort.existsByCongressIdAndUserId(congressId, memberId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(congressId, memberId, requester(ownerId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    org.assertj.core.api.Assertions.assertThat(apiException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    org.assertj.core.api.Assertions.assertThat(apiException.getCode()).isEqualTo("resource.not_found");
                });
    }

    @Test
    void shouldRejectUnrelatedCongressAdmin() {
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        CommitteeCongressSummary congress = congressSummary(congressId, ownerId);

        when(committeeCongressPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congress));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(
                eq(actorId),
                eq(congress.getInstitutionId()),
                eq("token")
        )).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(congressId, memberId, requester(actorId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    org.assertj.core.api.Assertions.assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    org.assertj.core.api.Assertions.assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
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
}
