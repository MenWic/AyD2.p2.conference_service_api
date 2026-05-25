package ayd2.p2b.conference_service_api.unit.feature.diploma.print_data;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.diploma.application.port.DiplomaRepositoryPort;
import ayd2.p2b.conference_service_api.feature.diploma.application.print_data.GetDiplomaPrintDataUseCase;
import ayd2.p2b.conference_service_api.feature.diploma.domain.model.DiplomaType;
import ayd2.p2b.conference_service_api.feature.diploma.dto.internal.DiplomaRequesterContext;
import ayd2.p2b.conference_service_api.feature.diploma.dto.response.DiplomaPrintDataResponse;
import ayd2.p2b.conference_service_api.feature.diploma.dto.response.DiplomaResponse;
import ayd2.p2b.conference_service_api.integration.dto.IamUserSummary;
import ayd2.p2b.conference_service_api.integration.exception.IntegrationExceptions;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDiplomaPrintDataUseCaseTest {

    @Mock
    private DiplomaRepositoryPort diplomaRepositoryPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;

    private GetDiplomaPrintDataUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetDiplomaPrintDataUseCase(diplomaRepositoryPort, iamUserLookupPort);
    }

    @Test
    void ownerShouldGetPrintDataWithUserFullNameFromIam() {
        UUID diplomaId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String accessToken = "raw-token";
        DiplomaResponse response = participationResponse(diplomaId, userId);

        when(diplomaRepositoryPort.findResponseById(diplomaId)).thenReturn(Optional.of(response));
        when(iamUserLookupPort.getUsersSummary(Set.of(userId), accessToken))
                .thenReturn(Map.of(userId, IamUserSummary.builder().id(userId).fullName("María Pérez").build()));

        DiplomaPrintDataResponse result = useCase.execute(diplomaId, requester(userId, accessToken));

        assertThat(result.getDiplomaId()).isEqualTo(diplomaId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getUserFullName()).isEqualTo("María Pérez");
    }

    @Test
    void nonOwnerShouldGetForbidden() {
        UUID diplomaId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        DiplomaResponse response = participationResponse(diplomaId, ownerId);

        when(diplomaRepositoryPort.findResponseById(diplomaId)).thenReturn(Optional.of(response));

        assertThatThrownBy(() -> useCase.execute(diplomaId, requester(requesterId, "raw-token")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
                });
    }

    @Test
    void missingDiplomaShouldGetNotFound() {
        UUID diplomaId = UUID.randomUUID();
        when(diplomaRepositoryPort.findResponseById(diplomaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(diplomaId, requester(UUID.randomUUID(), "raw-token")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiException.getCode()).isEqualTo("resource.not_found");
                });
    }

    @Test
    void iamUnavailableShouldPropagateAs503() {
        UUID diplomaId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String accessToken = "raw-token";
        DiplomaResponse response = participationResponse(diplomaId, userId);

        when(diplomaRepositoryPort.findResponseById(diplomaId)).thenReturn(Optional.of(response));
        when(iamUserLookupPort.getUsersSummary(Set.of(userId), accessToken))
                .thenThrow(IntegrationExceptions.iamUnavailable());

        assertThatThrownBy(() -> useCase.execute(diplomaId, requester(userId, accessToken)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
    }

    @Test
    void missingIamSummaryShouldReturn503() {
        UUID diplomaId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String accessToken = "raw-token";
        DiplomaResponse response = participationResponse(diplomaId, userId);

        when(diplomaRepositoryPort.findResponseById(diplomaId)).thenReturn(Optional.of(response));
        when(iamUserLookupPort.getUsersSummary(Set.of(userId), accessToken))
                .thenReturn(Map.of());

        assertThatThrownBy(() -> useCase.execute(diplomaId, requester(userId, accessToken)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
    }

    @Test
    void participationShouldReturnNullActivityFields() {
        UUID diplomaId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String accessToken = "raw-token";
        DiplomaResponse response = participationResponse(diplomaId, userId);

        when(diplomaRepositoryPort.findResponseById(diplomaId)).thenReturn(Optional.of(response));
        when(iamUserLookupPort.getUsersSummary(Set.of(userId), accessToken))
                .thenReturn(Map.of(userId, IamUserSummary.builder().id(userId).fullName("María Pérez").build()));

        DiplomaPrintDataResponse result = useCase.execute(diplomaId, requester(userId, accessToken));

        assertThat(result.getType()).isEqualTo(DiplomaType.PARTICIPATION);
        assertThat(result.getActivityId()).isNull();
        assertThat(result.getActivityName()).isNull();
    }

    @Test
    void leadershipShouldReturnActivityFields() {
        UUID diplomaId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        String accessToken = "raw-token";
        DiplomaResponse response = DiplomaResponse.builder()
                .id(diplomaId)
                .userId(userId)
                .congressId(UUID.randomUUID())
                .type(DiplomaType.LEADERSHIP)
                .activityId(activityId)
                .issuedAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .congressName("Congreso")
                .activityName("Actividad Liderada")
                .available(true)
                .build();

        when(diplomaRepositoryPort.findResponseById(diplomaId)).thenReturn(Optional.of(response));
        when(iamUserLookupPort.getUsersSummary(eq(Set.of(userId)), eq(accessToken)))
                .thenReturn(Map.of(userId, IamUserSummary.builder().id(userId).fullName("María Pérez").build()));

        DiplomaPrintDataResponse result = useCase.execute(diplomaId, requester(userId, accessToken));

        assertThat(result.getType()).isEqualTo(DiplomaType.LEADERSHIP);
        assertThat(result.getActivityId()).isEqualTo(activityId);
        assertThat(result.getActivityName()).isEqualTo("Actividad Liderada");
    }

    private DiplomaRequesterContext requester(UUID userId, String accessToken) {
        return DiplomaRequesterContext.builder()
                .userId(userId)
                .roles(Set.of(Role.PARTICIPANT))
                .accessToken(accessToken)
                .build();
    }

    private DiplomaResponse participationResponse(UUID diplomaId, UUID userId) {
        return DiplomaResponse.builder()
                .id(diplomaId)
                .userId(userId)
                .congressId(UUID.randomUUID())
                .type(DiplomaType.PARTICIPATION)
                .issuedAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .congressName("Congreso")
                .activityName(null)
                .available(true)
                .build();
    }
}
