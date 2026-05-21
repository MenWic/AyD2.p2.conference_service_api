package ayd2.p2b.conference_service_api.unit.feature.room.create;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.congress.application.exception.CongressExceptions;
import ayd2.p2b.conference_service_api.feature.room.application.create.CreateRoomUseCase;
import ayd2.p2b.conference_service_api.feature.room.application.port.RoomCongressPort;
import ayd2.p2b.conference_service_api.feature.room.application.port.RoomRepositoryPort;
import ayd2.p2b.conference_service_api.feature.room.domain.model.Room;
import ayd2.p2b.conference_service_api.feature.room.dto.internal.RoomCongressSummary;
import ayd2.p2b.conference_service_api.feature.room.dto.internal.RoomRequesterContext;
import ayd2.p2b.conference_service_api.feature.room.dto.request.CreateRoomRequest;
import ayd2.p2b.conference_service_api.feature.room.dto.response.RoomResponse;
import ayd2.p2b.conference_service_api.feature.room.mapper.RoomMapper;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateRoomUseCaseTest {

    @Mock
    private RoomRepositoryPort roomRepositoryPort;
    @Mock
    private RoomCongressPort roomCongressPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;
    @Mock
    private RoomMapper roomMapper;

    private CreateRoomUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateRoomUseCase(
                roomRepositoryPort,
                roomCongressPort,
                iamUserLookupPort,
                roomMapper
        );
    }

    @Test
    void shouldCreateRoomForOwnerCongressAdmin() {
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        RoomCongressSummary congress = congressSummary(congressId, ownerId);
        CreateRoomRequest request = CreateRoomRequest.builder()
                .name("  Sala Magna  ")
                .capacity(120)
                .location("  Edificio A ")
                .build();

        when(roomCongressPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congress));
        when(roomRepositoryPort.existsByCongressIdAndName(congressId, "Sala Magna")).thenReturn(false);
        when(roomRepositoryPort.save(any())).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            return room.toBuilder().id(UUID.randomUUID()).build();
        });
        when(roomMapper.toResponse(any())).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            return RoomResponse.builder()
                    .id(room.getId())
                    .congressId(room.getCongressId())
                    .name(room.getName())
                    .capacity(room.getCapacity())
                    .location(room.getLocation())
                    .build();
        });

        RoomResponse response = useCase.execute(congressId, request, requester(ownerId, Set.of(Role.CONGRESS_ADMIN)));

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepositoryPort).save(captor.capture());
        Room saved = captor.getValue();

        verify(iamUserLookupPort, never()).isCongressAdminLinkedToInstitution(any(), any(), any());
        assertThat(saved.getCreatedBy()).isEqualTo(ownerId);
        assertThat(saved.getName()).isEqualTo("Sala Magna");
        assertThat(saved.getLocation()).isEqualTo("Edificio A");
        assertThat(response.getCongressId()).isEqualTo(congressId);
    }

    @Test
    void shouldCreateRoomForLinkedCongressAdmin() {
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        RoomCongressSummary congress = congressSummary(congressId, ownerId);
        CreateRoomRequest request = CreateRoomRequest.builder().name("Sala B").build();

        when(roomCongressPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congress));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(actorId, congress.getInstitutionId(), "token")).thenReturn(true);
        when(roomRepositoryPort.existsByCongressIdAndName(congressId, "Sala B")).thenReturn(false);
        when(roomRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomMapper.toResponse(any())).thenReturn(RoomResponse.builder().name("Sala B").congressId(congressId).build());

        RoomResponse response = useCase.execute(congressId, request, requester(actorId, Set.of(Role.CONGRESS_ADMIN)));

        verify(iamUserLookupPort).isCongressAdminLinkedToInstitution(actorId, congress.getInstitutionId(), "token");
        assertThat(response.getName()).isEqualTo("Sala B");
    }

    @Test
    void shouldRejectDuplicateNameInSameCongress() {
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(roomCongressPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congressSummary(congressId, ownerId)));
        when(roomRepositoryPort.existsByCongressIdAndName(congressId, "Sala A")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(
                congressId,
                CreateRoomRequest.builder().name("Sala A").build(),
                requester(ownerId, Set.of(Role.CONGRESS_ADMIN))
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertConflict((ApiException) ex));
    }

    @Test
    void shouldAllowSameNameInDifferentCongressesByScopedDuplicateCheck() {
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(roomCongressPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congressSummary(congressId, ownerId)));
        when(roomRepositoryPort.existsByCongressIdAndName(congressId, "Sala Compartida")).thenReturn(false);
        when(roomRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomMapper.toResponse(any())).thenReturn(RoomResponse.builder().name("Sala Compartida").build());

        useCase.execute(
                congressId,
                CreateRoomRequest.builder().name("Sala Compartida").build(),
                requester(ownerId, Set.of(Role.CONGRESS_ADMIN))
        );

        verify(roomRepositoryPort).existsByCongressIdAndName(congressId, "Sala Compartida");
    }

    @Test
    void shouldRejectCapacityLessOrEqualThanZero() {
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(roomCongressPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congressSummary(congressId, ownerId)));

        assertThatThrownBy(() -> useCase.execute(
                congressId,
                CreateRoomRequest.builder().name("Sala").capacity(0).build(),
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
    void shouldRejectNonCongressAdmin() {
        assertThatThrownBy(() -> useCase.execute(
                UUID.randomUUID(),
                CreateRoomRequest.builder().name("Sala").build(),
                requester(UUID.randomUUID(), Set.of(Role.PARTICIPANT))
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertForbidden((ApiException) ex));
    }

    @Test
    void shouldPropagateIamTechnicalFailure() {
        UUID congressId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        RoomCongressSummary congress = congressSummary(congressId, ownerId);

        when(roomCongressPort.findManageableCongressById(congressId)).thenReturn(Optional.of(congress));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(actorId, congress.getInstitutionId(), "token"))
                .thenThrow(CongressExceptions.iamUnavailable());

        assertThatThrownBy(() -> useCase.execute(
                congressId,
                CreateRoomRequest.builder().name("Sala").build(),
                requester(actorId, Set.of(Role.CONGRESS_ADMIN))
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
    }

    private RoomRequesterContext requester(UUID userId, Set<Role> roles) {
        return RoomRequesterContext.builder()
                .userId(userId)
                .roles(roles)
                .accessToken("token")
                .build();
    }

    private RoomCongressSummary congressSummary(UUID congressId, UUID createdBy) {
        return RoomCongressSummary.builder()
                .id(congressId)
                .institutionId(UUID.randomUUID())
                .createdBy(createdBy)
                .build();
    }

    private void assertForbidden(ApiException ex) {
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getCode()).isEqualTo("auth.forbidden");
    }

    private void assertConflict(ApiException ex) {
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getCode()).isEqualTo("resource.conflict");
    }
}
