package ayd2.p2b.conference_service_api.unit.feature.call.open;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.call.application.open.OpenCallUseCase;
import ayd2.p2b.conference_service_api.feature.call.application.port.CallCongressPort;
import ayd2.p2b.conference_service_api.feature.call.application.port.CallRepositoryPort;
import ayd2.p2b.conference_service_api.feature.call.domain.model.Call;
import ayd2.p2b.conference_service_api.feature.call.domain.model.CallStatus;
import ayd2.p2b.conference_service_api.feature.call.dto.internal.CallCongressSummary;
import ayd2.p2b.conference_service_api.feature.call.dto.internal.CallRequesterContext;
import ayd2.p2b.conference_service_api.feature.call.dto.response.CallResponse;
import ayd2.p2b.conference_service_api.feature.call.mapper.CallMapper;
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
class OpenCallUseCaseTest {

    @Mock
    private CallRepositoryPort callRepositoryPort;
    @Mock
    private CallCongressPort callCongressPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;
    @Mock
    private CallMapper callMapper;

    private OpenCallUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new OpenCallUseCase(callRepositoryPort, callCongressPort, iamUserLookupPort, callMapper);
    }

    @Test
    void shouldOpenCallForOwnerCongressAdmin() {
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        CallCongressSummary congress = congressSummary(congressId, ownerId);
        Call saved = Call.builder()
                .id(UUID.randomUUID())
                .congressId(congressId)
                .status(CallStatus.OPEN)
                .openedAt(OffsetDateTime.now())
                .createdBy(ownerId)
                .build();

        when(callCongressPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congress));
        when(callRepositoryPort.existsOpenCallByCongressId(congressId)).thenReturn(false);
        when(callRepositoryPort.save(any())).thenReturn(saved);
        when(callMapper.toResponse(saved)).thenReturn(CallResponse.builder()
                .id(saved.getId())
                .congressId(congressId)
                .status(CallStatus.OPEN)
                .openedAt(saved.getOpenedAt())
                .build());

        CallResponse response = useCase.execute(congressId, requester(ownerId, Set.of(Role.CONGRESS_ADMIN)));

        verify(iamUserLookupPort, never()).isCongressAdminLinkedToInstitution(any(), any(), any());
        assertThat(response.getId()).isEqualTo(saved.getId());
        assertThat(response.getStatus()).isEqualTo(CallStatus.OPEN);
    }

    @Test
    void shouldRejectNonScopedCongressAdmin() {
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        CallCongressSummary congress = congressSummary(congressId, ownerId);

        when(callCongressPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congress));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(
                eq(actorId),
                eq(congress.getInstitutionId()),
                eq("token")
        )).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(congressId, requester(actorId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
                });
    }

    @Test
    void shouldRejectSecondOpenCallForCongress() {
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        when(callCongressPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congressSummary(congressId, ownerId)));
        when(callRepositoryPort.existsOpenCallByCongressId(congressId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(congressId, requester(ownerId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("resource.conflict");
                });
    }

    private CallRequesterContext requester(UUID userId, Set<Role> roles) {
        return CallRequesterContext.builder()
                .userId(userId)
                .roles(roles)
                .accessToken("token")
                .build();
    }

    private CallCongressSummary congressSummary(UUID congressId, UUID ownerId) {
        return CallCongressSummary.builder()
                .congressId(congressId)
                .institutionId(UUID.randomUUID())
                .createdBy(ownerId)
                .build();
    }
}
