package ayd2.p2b.conference_service_api.unit.feature.activity.delete;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.activity.application.delete.DeleteActivityUseCase;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityCongressRoomPort;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityDependencyPort;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityLeaderRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.Activity;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeader;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeaderType;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivityCongressRoomSummary;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivityRequesterContext;
import ayd2.p2b.conference_service_api.feature.activity.dto.response.ActivityResponse;
import ayd2.p2b.conference_service_api.feature.activity.mapper.ActivityMapper;
import ayd2.p2b.conference_service_api.integration.exception.IntegrationExceptions;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteActivityUseCaseTest {

    @Mock
    private ActivityRepositoryPort activityRepositoryPort;
    @Mock
    private ActivityLeaderRepositoryPort activityLeaderRepositoryPort;
    @Mock
    private ActivityCongressRoomPort activityCongressRoomPort;
    @Mock
    private ActivityDependencyPort activityDependencyPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;
    @Mock
    private ActivityMapper activityMapper;

    private DeleteActivityUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteActivityUseCase(
                activityRepositoryPort,
                activityLeaderRepositoryPort,
                activityCongressRoomPort,
                activityDependencyPort,
                iamUserLookupPort,
                activityMapper
        );
    }

    @Test
    void shouldBlockDeleteWhenDependenciesExist() {
        Activity current = activity();
        UUID ownerId = current.getCreatedBy();
        when(activityRepositoryPort.findById(current.getId())).thenReturn(Optional.of(current));
        when(activityCongressRoomPort.findManageableCongressById(current.getCongressId()))
                .thenReturn(Optional.of(summary(current.getCongressId(), ownerId)));
        when(activityDependencyPort.findBlockingDependencies(current.getId())).thenReturn(List.of("reservations"));

        assertThatThrownBy(() -> useCase.execute(current.getId(), requester(ownerId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("resource.conflict");
                });
    }

    @Test
    void shouldBlockDeleteWhenAttendanceDependenciesExist() {
        Activity current = activity();
        UUID ownerId = current.getCreatedBy();
        when(activityRepositoryPort.findById(current.getId())).thenReturn(Optional.of(current));
        when(activityCongressRoomPort.findManageableCongressById(current.getCongressId()))
                .thenReturn(Optional.of(summary(current.getCongressId(), ownerId)));
        when(activityDependencyPort.findBlockingDependencies(current.getId())).thenReturn(List.of("attendances"));

        assertThatThrownBy(() -> useCase.execute(current.getId(), requester(ownerId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("resource.conflict");
                });
    }

    @Test
    void shouldDeleteWhenNoDependencies() {
        Activity current = activity();
        UUID ownerId = current.getCreatedBy();
        when(activityRepositoryPort.findById(current.getId())).thenReturn(Optional.of(current));
        when(activityCongressRoomPort.findManageableCongressById(current.getCongressId()))
                .thenReturn(Optional.of(summary(current.getCongressId(), ownerId)));
        when(activityDependencyPort.findBlockingDependencies(current.getId())).thenReturn(List.of());
        UUID leaderId = UUID.randomUUID();
        when(activityLeaderRepositoryPort.findByActivityId(current.getId())).thenReturn(List.of(
                ActivityLeader.builder().userId(leaderId).leaderType(ActivityLeaderType.SPEAKER).build()
        ));
        when(activityMapper.toResponse(any())).thenAnswer(invocation -> {
            Activity deleted = invocation.getArgument(0);
            return ActivityResponse.builder().id(deleted.getId()).build();
        });

        ActivityResponse response = useCase.execute(current.getId(), requester(ownerId, Set.of(Role.CONGRESS_ADMIN)));

        verify(activityRepositoryPort).deleteById(current.getId());
        assertThat(response.getId()).isEqualTo(current.getId());
        assertThat(response.getLeaders()).containsExactly(leaderId);
    }

    @Test
    void shouldPropagateIamTechnicalFailure() {
        Activity current = activity();
        UUID actorId = UUID.randomUUID();
        ActivityCongressRoomSummary summary = summary(current.getCongressId(), current.getCreatedBy());
        when(activityRepositoryPort.findById(current.getId())).thenReturn(Optional.of(current));
        when(activityCongressRoomPort.findManageableCongressById(current.getCongressId())).thenReturn(Optional.of(summary));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(actorId, summary.getInstitutionId(), "token"))
                .thenThrow(IntegrationExceptions.iamUnavailable());

        assertThatThrownBy(() -> useCase.execute(current.getId(), requester(actorId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
    }

    private Activity activity() {
        return Activity.builder()
                .id(UUID.randomUUID())
                .congressId(UUID.randomUUID())
                .roomId(UUID.randomUUID())
                .name("Ponencia")
                .description("Descripcion")
                .type(ActivityType.PONENCIA)
                .startTime(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .endTime(OffsetDateTime.parse("2026-10-10T11:00:00Z"))
                .createdBy(UUID.randomUUID())
                .build();
    }

    private ActivityCongressRoomSummary summary(UUID congressId, UUID ownerId) {
        return ActivityCongressRoomSummary.builder()
                .congressId(congressId)
                .institutionId(UUID.randomUUID())
                .createdBy(ownerId)
                .build();
    }

    private ActivityRequesterContext requester(UUID userId, Set<Role> roles) {
        return ActivityRequesterContext.builder()
                .userId(userId)
                .roles(roles)
                .accessToken("token")
                .build();
    }
}
