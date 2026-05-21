package ayd2.p2b.conference_service_api.unit.feature.activity.update;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.activity.application.leader.ActivityLeaderResolver;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityCongressRoomPort;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityLeaderRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.application.update.UpdateActivityUseCase;
import ayd2.p2b.conference_service_api.feature.activity.domain.exception.ActivityDomainException;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.Activity;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeader;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeaderType;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivityCongressRoomSummary;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivityRequesterContext;
import ayd2.p2b.conference_service_api.feature.activity.dto.request.UpdateActivityRequest;
import ayd2.p2b.conference_service_api.feature.activity.dto.response.ActivityResponse;
import ayd2.p2b.conference_service_api.feature.activity.mapper.ActivityMapper;
import ayd2.p2b.conference_service_api.integration.exception.IntegrationExceptions;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateActivityUseCaseTest {

    @Mock
    private ActivityRepositoryPort activityRepositoryPort;
    @Mock
    private ActivityLeaderRepositoryPort activityLeaderRepositoryPort;
    @Mock
    private ActivityLeaderResolver activityLeaderResolver;
    @Mock
    private ActivityCongressRoomPort activityCongressRoomPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;
    @Mock
    private ActivityMapper activityMapper;

    private UpdateActivityUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateActivityUseCase(
                activityRepositoryPort,
                activityLeaderRepositoryPort,
                activityLeaderResolver,
                activityCongressRoomPort,
                iamUserLookupPort,
                activityMapper
        );
    }

    @Test
    void shouldUpdateAllowedFields() {
        Activity current = existingActivity();
        UUID ownerId = current.getCreatedBy();
        UUID roomId = UUID.randomUUID();
        when(activityRepositoryPort.findById(current.getId())).thenReturn(Optional.of(current));
        when(activityCongressRoomPort.findManageableCongressById(current.getCongressId()))
                .thenReturn(Optional.of(congressSummary(current.getCongressId(), ownerId)));
        when(activityCongressRoomPort.findManageableRoomById(roomId))
                .thenReturn(Optional.of(roomSummary(current.getCongressId(), roomId, ownerId)));
        when(activityRepositoryPort.existsRoomTimeOverlap(roomId, startLater(), endLater(), current.getId())).thenReturn(false);
        when(activityRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(activityLeaderRepositoryPort.findByActivityId(current.getId())).thenReturn(List.of());
        when(activityMapper.toResponse(any())).thenReturn(ActivityResponse.builder().build());

        useCase.execute(current.getId(), UpdateActivityRequest.builder()
                .name("  Taller actualizado ")
                .description("  Nueva descripcion ")
                .roomId(roomId)
                .startTime(startLater())
                .endTime(endLater())
                .workshopCapacity(40)
                .build(), requester(ownerId, Set.of(Role.CONGRESS_ADMIN)));

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepositoryPort).save(captor.capture());
        Activity saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Taller actualizado");
        assertThat(saved.getDescription()).isEqualTo("Nueva descripcion");
        assertThat(saved.getRoomId()).isEqualTo(roomId);
        assertThat(saved.getWorkshopCapacity()).isEqualTo(40);
    }

    @Test
    void shouldRejectTypeChange() {
        Activity current = existingActivity();
        UUID ownerId = current.getCreatedBy();
        when(activityRepositoryPort.findById(current.getId())).thenReturn(Optional.of(current));
        when(activityCongressRoomPort.findManageableCongressById(current.getCongressId()))
                .thenReturn(Optional.of(congressSummary(current.getCongressId(), ownerId)));

        assertThatThrownBy(() -> useCase.execute(
                current.getId(),
                UpdateActivityRequest.builder().type(ActivityType.PONENCIA).build(),
                requester(ownerId, Set.of(Role.CONGRESS_ADMIN))
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiException.getCode()).isEqualTo("validation.failed");
                });
    }

    @Test
    void shouldValidateOverlapExcludingCurrentActivity() {
        Activity current = existingActivity();
        UUID ownerId = current.getCreatedBy();
        when(activityRepositoryPort.findById(current.getId())).thenReturn(Optional.of(current));
        when(activityCongressRoomPort.findManageableCongressById(current.getCongressId()))
                .thenReturn(Optional.of(congressSummary(current.getCongressId(), ownerId)));
        when(activityRepositoryPort.existsRoomTimeOverlap(current.getRoomId(), startLater(), endLater(), current.getId())).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(
                current.getId(),
                UpdateActivityRequest.builder().startTime(startLater()).endTime(endLater()).build(),
                requester(ownerId, Set.of(Role.CONGRESS_ADMIN))
        )).isInstanceOf(ActivityDomainException.class);
    }

    @Test
    void shouldRejectRoomFromDifferentCongress() {
        Activity current = existingActivity();
        UUID ownerId = current.getCreatedBy();
        UUID newRoomId = UUID.randomUUID();
        when(activityRepositoryPort.findById(current.getId())).thenReturn(Optional.of(current));
        when(activityCongressRoomPort.findManageableCongressById(current.getCongressId()))
                .thenReturn(Optional.of(congressSummary(current.getCongressId(), ownerId)));
        when(activityCongressRoomPort.findManageableRoomById(newRoomId))
                .thenReturn(Optional.of(roomSummary(UUID.randomUUID(), newRoomId, ownerId)));

        assertThatThrownBy(() -> useCase.execute(
                current.getId(),
                UpdateActivityRequest.builder().roomId(newRoomId).build(),
                requester(ownerId, Set.of(Role.CONGRESS_ADMIN))
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiException.getCode()).isEqualTo("validation.failed");
                });
    }

    @Test
    void shouldReplaceLeadersWhenRequestContainsLeaders() {
        Activity current = existingActivity();
        UUID leaderId = UUID.randomUUID();
        List<ActivityLeader> replacementLeaders = List.of(ActivityLeader.builder()
                .userId(leaderId)
                .leaderType(ActivityLeaderType.WORKSHOP_LEADER)
                .build());
        when(activityRepositoryPort.findById(current.getId())).thenReturn(Optional.of(current));
        when(activityCongressRoomPort.findManageableCongressById(current.getCongressId()))
                .thenReturn(Optional.of(congressSummary(current.getCongressId(), current.getCreatedBy())));
        when(activityRepositoryPort.existsRoomTimeOverlap(current.getRoomId(), current.getStartTime(), current.getEndTime(), current.getId()))
                .thenReturn(false);
        when(activityRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(activityLeaderResolver.resolve(eq(ActivityType.TALLER), eq(List.of(leaderId)), eq("token")))
                .thenReturn(replacementLeaders);
        when(activityLeaderRepositoryPort.findByActivityId(current.getId())).thenReturn(replacementLeaders);
        when(activityMapper.toResponse(any())).thenReturn(ActivityResponse.builder().build());

        ActivityResponse response = useCase.execute(
                current.getId(),
                UpdateActivityRequest.builder().leaders(List.of(leaderId)).build(),
                requester(current.getCreatedBy(), Set.of(Role.CONGRESS_ADMIN))
        );

        verify(activityLeaderRepositoryPort).replaceLeaders(current.getId(), replacementLeaders);
        assertThat(response.getLeaders()).containsExactly(leaderId);
    }

    @Test
    void shouldPreserveLeadersWhenRequestLeadersIsNull() {
        Activity current = existingActivity();
        UUID leaderId = UUID.randomUUID();
        List<ActivityLeader> currentLeaders = List.of(ActivityLeader.builder()
                .userId(leaderId)
                .leaderType(ActivityLeaderType.WORKSHOP_LEADER)
                .build());
        when(activityRepositoryPort.findById(current.getId())).thenReturn(Optional.of(current));
        when(activityCongressRoomPort.findManageableCongressById(current.getCongressId()))
                .thenReturn(Optional.of(congressSummary(current.getCongressId(), current.getCreatedBy())));
        when(activityRepositoryPort.existsRoomTimeOverlap(current.getRoomId(), current.getStartTime(), current.getEndTime(), current.getId()))
                .thenReturn(false);
        when(activityRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(activityLeaderRepositoryPort.findByActivityId(current.getId())).thenReturn(currentLeaders);
        when(activityMapper.toResponse(any())).thenReturn(ActivityResponse.builder().build());

        ActivityResponse response = useCase.execute(
                current.getId(),
                UpdateActivityRequest.builder().name("Nuevo").build(),
                requester(current.getCreatedBy(), Set.of(Role.CONGRESS_ADMIN))
        );

        assertThat(response.getLeaders()).containsExactly(leaderId);
    }

    @Test
    void shouldPropagateIamTechnicalFailure() {
        Activity current = existingActivity();
        UUID actorId = UUID.randomUUID();
        ActivityCongressRoomSummary congressSummary = congressSummary(current.getCongressId(), current.getCreatedBy());

        when(activityRepositoryPort.findById(current.getId())).thenReturn(Optional.of(current));
        when(activityCongressRoomPort.findManageableCongressById(current.getCongressId()))
                .thenReturn(Optional.of(congressSummary));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(actorId, congressSummary.getInstitutionId(), "token"))
                .thenThrow(IntegrationExceptions.iamUnavailable());

        assertThatThrownBy(() -> useCase.execute(current.getId(), UpdateActivityRequest.builder().build(), requester(actorId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
    }

    private Activity existingActivity() {
        return Activity.builder()
                .id(UUID.randomUUID())
                .congressId(UUID.randomUUID())
                .roomId(UUID.randomUUID())
                .name("Taller")
                .description("Descripcion")
                .type(ActivityType.TALLER)
                .startTime(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .endTime(OffsetDateTime.parse("2026-10-10T11:00:00Z"))
                .workshopCapacity(30)
                .createdBy(UUID.randomUUID())
                .build();
    }

    private ActivityCongressRoomSummary congressSummary(UUID congressId, UUID ownerId) {
        return ActivityCongressRoomSummary.builder()
                .congressId(congressId)
                .institutionId(UUID.randomUUID())
                .createdBy(ownerId)
                .build();
    }

    private ActivityCongressRoomSummary roomSummary(UUID congressId, UUID roomId, UUID ownerId) {
        return ActivityCongressRoomSummary.builder()
                .congressId(congressId)
                .roomId(roomId)
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

    private OffsetDateTime startLater() {
        return OffsetDateTime.parse("2026-10-10T12:00:00Z");
    }

    private OffsetDateTime endLater() {
        return OffsetDateTime.parse("2026-10-10T13:00:00Z");
    }
}
