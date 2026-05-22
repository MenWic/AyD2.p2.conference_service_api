package ayd2.p2b.conference_service_api.unit.feature.call.close;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.call.application.close.CloseCallUseCase;
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
class CloseCallUseCaseTest {

    @Mock
    private CallRepositoryPort callRepositoryPort;
    @Mock
    private CallCongressPort callCongressPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;
    @Mock
    private CallMapper callMapper;

    private CloseCallUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CloseCallUseCase(callRepositoryPort, callCongressPort, iamUserLookupPort, callMapper);
    }

    @Test
    void shouldCloseOpenCallAndSetClosedAt() {
        UUID callId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Call existing = Call.builder()
                .id(callId)
                .congressId(congressId)
                .status(CallStatus.OPEN)
                .openedAt(OffsetDateTime.now().minusDays(1))
                .createdBy(ownerId)
                .build();
        CallCongressSummary congress = congressSummary(congressId, ownerId);

        when(callRepositoryPort.findById(callId)).thenReturn(Optional.of(existing));
        when(callCongressPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congress));
        when(callRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(callMapper.toResponse(any())).thenAnswer(invocation -> {
            Call closed = invocation.getArgument(0);
            return CallResponse.builder()
                    .id(closed.getId())
                    .congressId(closed.getCongressId())
                    .status(closed.getStatus())
                    .openedAt(closed.getOpenedAt())
                    .closedAt(closed.getClosedAt())
                    .build();
        });

        CallResponse response = useCase.execute(callId, requester(ownerId, Set.of(Role.CONGRESS_ADMIN)));

        verify(iamUserLookupPort, never()).isCongressAdminLinkedToInstitution(any(), any(), any());
        assertThat(response.getStatus()).isEqualTo(CallStatus.CLOSED);
        assertThat(response.getClosedAt()).isNotNull();
    }

    @Test
    void shouldRejectClosingAlreadyClosedCall() {
        UUID callId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Call closed = Call.builder()
                .id(callId)
                .congressId(congressId)
                .status(CallStatus.CLOSED)
                .openedAt(OffsetDateTime.now().minusDays(2))
                .closedAt(OffsetDateTime.now().minusDays(1))
                .createdBy(ownerId)
                .build();

        when(callRepositoryPort.findById(callId)).thenReturn(Optional.of(closed));
        when(callCongressPort.findManageableCongressById(congressId))
                .thenReturn(Optional.of(congressSummary(congressId, ownerId)));

        assertThatThrownBy(() -> useCase.execute(callId, requester(ownerId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("resource.conflict");
                });
    }

    @Test
    void shouldRejectUnscopedCongressAdmin() {
        UUID callId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Call existing = Call.builder()
                .id(callId)
                .congressId(congressId)
                .status(CallStatus.OPEN)
                .openedAt(OffsetDateTime.now().minusDays(1))
                .createdBy(ownerId)
                .build();
        CallCongressSummary congress = congressSummary(congressId, ownerId);

        when(callRepositoryPort.findById(callId)).thenReturn(Optional.of(existing));
        when(callCongressPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congress));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(
                eq(actorId),
                eq(congress.getInstitutionId()),
                eq("token")
        )).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(callId, requester(actorId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
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
