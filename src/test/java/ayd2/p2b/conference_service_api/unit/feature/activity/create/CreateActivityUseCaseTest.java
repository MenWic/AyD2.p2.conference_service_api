package ayd2.p2b.conference_service_api.unit.feature.activity.create;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.activity.application.create.CreateActivityUseCase;
import ayd2.p2b.conference_service_api.feature.activity.application.leader.ActivityLeaderResolver;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityCongressRoomPort;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityLeaderRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.domain.exception.ActivityDomainException;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeader;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeaderType;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.Activity;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivityCongressRoomSummary;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivityRequesterContext;
import ayd2.p2b.conference_service_api.feature.activity.dto.request.CreateActivityRequest;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateActivityUseCaseTest {

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

    private CreateActivityUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateActivityUseCase(
                activityRepositoryPort,
                activityLeaderRepositoryPort,
                activityLeaderResolver,
                activityCongressRoomPort,
                iamUserLookupPort,
                activityMapper
        );
    }

    @Test
    void shouldCreateValidActivityForOwnerCongressAdmin() {
        UUID congressId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ActivityCongressRoomSummary congressSummary = congressSummary(congressId, null, ownerId);
        ActivityCongressRoomSummary roomSummary = roomSummary(congressId, roomId, ownerId);

        when(activityCongressRoomPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congressSummary));
        when(activityCongressRoomPort.findManageableRoomById(roomId)).thenReturn(Optional.of(roomSummary));
        when(activityRepositoryPort.existsRoomTimeOverlap(roomId, start(), end(), null)).thenReturn(false);
        when(activityRepositoryPort.save(any())).thenAnswer(invocation -> {
            Activity activity = invocation.getArgument(0);
            return activity.toBuilder().id(UUID.randomUUID()).build();
        });
        when(activityLeaderResolver.resolve(eq(ActivityType.TALLER), eq(List.of()), eq("token")))
                .thenReturn(List.of());
        when(activityMapper.toResponse(any())).thenAnswer(invocation -> {
            Activity activity = invocation.getArgument(0);
            return ActivityResponse.builder()
                    .id(activity.getId())
                    .congressId(activity.getCongressId())
                    .roomId(activity.getRoomId())
                    .name(activity.getName())
                    .build();
        });

        ActivityResponse response = useCase.execute(congressId, createRequest(roomId), requester(ownerId, Set.of(Role.CONGRESS_ADMIN)));

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepositoryPort).save(captor.capture());
        Activity saved = captor.getValue();
        verify(iamUserLookupPort, never()).isCongressAdminLinkedToInstitution(any(), any(), any());
        verify(activityLeaderRepositoryPort).replaceLeaders(eq(response.getId()), eq(List.of()));
        assertThat(saved.getCreatedBy()).isEqualTo(ownerId);
        assertThat(saved.getName()).isEqualTo("Taller Cloud");
        assertThat(response.getLeaders()).isEmpty();
    }

    @Test
    void shouldRejectMissingRoom() {
        UUID congressId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(activityCongressRoomPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congressSummary(congressId, null, ownerId)));
        when(activityCongressRoomPort.findManageableRoomById(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(congressId, createRequest(roomId), requester(ownerId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertNotFound((ApiException) ex));
    }

    @Test
    void shouldRejectRoomOutsideCongressPath() {
        UUID congressId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(activityCongressRoomPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congressSummary(congressId, null, ownerId)));
        when(activityCongressRoomPort.findManageableRoomById(roomId))
                .thenReturn(Optional.of(roomSummary(UUID.randomUUID(), roomId, ownerId)));

        assertThatThrownBy(() -> useCase.execute(congressId, createRequest(roomId), requester(ownerId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertValidation((ApiException) ex));
    }

    @Test
    void shouldRejectOverlapInSameRoom() {
        UUID congressId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(activityCongressRoomPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congressSummary(congressId, null, ownerId)));
        when(activityCongressRoomPort.findManageableRoomById(roomId)).thenReturn(Optional.of(roomSummary(congressId, roomId, ownerId)));
        when(activityRepositoryPort.existsRoomTimeOverlap(roomId, start(), end(), null)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(congressId, createRequest(roomId), requester(ownerId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ActivityDomainException.class);
    }

    @Test
    void shouldAllowOverlapInDifferentRoom() {
        UUID congressId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(activityCongressRoomPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congressSummary(congressId, null, ownerId)));
        when(activityCongressRoomPort.findManageableRoomById(roomId)).thenReturn(Optional.of(roomSummary(congressId, roomId, ownerId)));
        when(activityRepositoryPort.existsRoomTimeOverlap(roomId, start(), end(), null)).thenReturn(false);
        when(activityRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(activityLeaderResolver.resolve(eq(ActivityType.TALLER), eq(List.of()), eq("token")))
                .thenReturn(List.of());
        when(activityMapper.toResponse(any())).thenReturn(ActivityResponse.builder().leaders(List.of()).build());

        ActivityResponse response = useCase.execute(congressId, createRequest(roomId), requester(ownerId, Set.of(Role.CONGRESS_ADMIN)));

        assertThat(response.getLeaders()).isEmpty();
    }

    @Test
    void shouldPersistResolvedLeadersAndReturnTheirIds() {
        UUID congressId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID leaderId = UUID.randomUUID();
        ActivityCongressRoomSummary congressSummary = congressSummary(congressId, null, ownerId);
        ActivityCongressRoomSummary roomSummary = roomSummary(congressId, roomId, ownerId);

        when(activityCongressRoomPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congressSummary));
        when(activityCongressRoomPort.findManageableRoomById(roomId)).thenReturn(Optional.of(roomSummary));
        when(activityRepositoryPort.existsRoomTimeOverlap(roomId, start(), end(), null)).thenReturn(false);
        when(activityRepositoryPort.save(any())).thenAnswer(invocation -> {
            Activity activity = invocation.getArgument(0);
            return activity.toBuilder().id(UUID.randomUUID()).build();
        });
        when(activityLeaderResolver.resolve(eq(ActivityType.TALLER), eq(List.of(leaderId)), eq("token")))
                .thenReturn(List.of(ActivityLeader.builder()
                        .userId(leaderId)
                        .leaderType(ActivityLeaderType.WORKSHOP_LEADER)
                        .build()));
        when(activityMapper.toResponse(any())).thenAnswer(invocation -> {
            Activity activity = invocation.getArgument(0);
            return ActivityResponse.builder().id(activity.getId()).build();
        });

        ActivityResponse response = useCase.execute(
                congressId,
                CreateActivityRequest.builder()
                        .name("Taller")
                        .description("Descripcion")
                        .roomId(roomId)
                        .type(ActivityType.TALLER)
                        .startTime(start())
                        .endTime(end())
                        .workshopCapacity(30)
                        .leaders(List.of(leaderId))
                        .build(),
                requester(ownerId, Set.of(Role.CONGRESS_ADMIN))
        );

        assertThat(response.getLeaders()).containsExactly(leaderId);
    }

    @Test
    void shouldPropagateIamTechnicalFailure() {
        UUID congressId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        when(activityCongressRoomPort.findManageableCongressById(congressId))
                .thenReturn(Optional.of(congressSummary(congressId, institutionId, ownerId)));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(actorId, institutionId, "token"))
                .thenThrow(IntegrationExceptions.iamUnavailable());

        assertThatThrownBy(() -> useCase.execute(congressId, createRequest(roomId), requester(actorId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
    }

    private CreateActivityRequest createRequest(UUID roomId) {
        return CreateActivityRequest.builder()
                .name("  Taller Cloud ")
                .description("  Descripcion ")
                .roomId(roomId)
                .type(ActivityType.TALLER)
                .startTime(start())
                .endTime(end())
                .workshopCapacity(30)
                .leaders(List.of())
                .build();
    }

    private ActivityRequesterContext requester(UUID userId, Set<Role> roles) {
        return ActivityRequesterContext.builder()
                .userId(userId)
                .roles(roles)
                .accessToken("token")
                .build();
    }

    private ActivityCongressRoomSummary congressSummary(UUID congressId, UUID institutionId, UUID createdBy) {
        return ActivityCongressRoomSummary.builder()
                .congressId(congressId)
                .institutionId(institutionId == null ? UUID.randomUUID() : institutionId)
                .createdBy(createdBy)
                .build();
    }

    private ActivityCongressRoomSummary roomSummary(UUID congressId, UUID roomId, UUID createdBy) {
        return ActivityCongressRoomSummary.builder()
                .congressId(congressId)
                .institutionId(UUID.randomUUID())
                .roomId(roomId)
                .createdBy(createdBy)
                .build();
    }

    private OffsetDateTime start() {
        return OffsetDateTime.parse("2026-10-10T10:00:00Z");
    }

    private OffsetDateTime end() {
        return OffsetDateTime.parse("2026-10-10T11:00:00Z");
    }

    private void assertValidation(ApiException ex) {
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getCode()).isEqualTo("validation.failed");
    }

    private void assertNotFound(ApiException ex) {
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getCode()).isEqualTo("resource.not_found");
    }
}
