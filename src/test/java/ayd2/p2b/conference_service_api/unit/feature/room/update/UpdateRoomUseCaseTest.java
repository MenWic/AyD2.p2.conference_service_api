package ayd2.p2b.conference_service_api.unit.feature.room.update;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.congress.application.exception.CongressExceptions;
import ayd2.p2b.conference_service_api.feature.room.application.port.RoomCongressPort;
import ayd2.p2b.conference_service_api.feature.room.application.port.RoomRepositoryPort;
import ayd2.p2b.conference_service_api.feature.room.application.update.UpdateRoomUseCase;
import ayd2.p2b.conference_service_api.feature.room.domain.model.Room;
import ayd2.p2b.conference_service_api.feature.room.dto.internal.RoomCongressSummary;
import ayd2.p2b.conference_service_api.feature.room.dto.internal.RoomRequesterContext;
import ayd2.p2b.conference_service_api.feature.room.dto.request.UpdateRoomRequest;
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
class UpdateRoomUseCaseTest {

    @Mock
    private RoomRepositoryPort roomRepositoryPort;
    @Mock
    private RoomCongressPort roomCongressPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;
    @Mock
    private RoomMapper roomMapper;

    private UpdateRoomUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateRoomUseCase(
                roomRepositoryPort,
                roomCongressPort,
                iamUserLookupPort,
                roomMapper
        );
    }

    @Test
    void shouldUpdateNameCapacityAndLocation() {
        Room current = existingRoom();
        UUID roomId = current.getId();
        UUID actorId = current.getCreatedBy();

        when(roomRepositoryPort.findById(roomId)).thenReturn(Optional.of(current));
        when(roomCongressPort.findManageableCongressById(current.getCongressId()))
                .thenReturn(Optional.of(congressSummary(current.getCongressId(), actorId)));
        when(roomRepositoryPort.existsByCongressIdAndNameAndIdNot(current.getCongressId(), "Sala Actualizada", roomId)).thenReturn(false);
        when(roomRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
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

        UpdateRoomRequest request = UpdateRoomRequest.builder()
                .name("  Sala Actualizada ")
                .capacity(150)
                .location("  Torre 1  ")
                .build();

        RoomResponse response = useCase.execute(roomId, request, requester(actorId, Set.of(Role.CONGRESS_ADMIN)));

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepositoryPort).save(captor.capture());
        Room saved = captor.getValue();

        assertThat(saved.getName()).isEqualTo("Sala Actualizada");
        assertThat(saved.getCapacity()).isEqualTo(150);
        assertThat(saved.getLocation()).isEqualTo("Torre 1");
        assertThat(saved.getUpdatedBy()).isEqualTo(actorId);
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(response.getName()).isEqualTo("Sala Actualizada");
    }

    @Test
    void shouldKeepCurrentValuesWhenUpdateFieldsAreNull() {
        Room current = existingRoom();
        UUID actorId = current.getCreatedBy();

        when(roomRepositoryPort.findById(current.getId())).thenReturn(Optional.of(current));
        when(roomCongressPort.findManageableCongressById(current.getCongressId()))
                .thenReturn(Optional.of(congressSummary(current.getCongressId(), actorId)));
        when(roomRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomMapper.toResponse(any())).thenReturn(RoomResponse.builder().name("Sala A").capacity(100).location("Edificio A").build());

        useCase.execute(current.getId(), UpdateRoomRequest.builder().build(), requester(actorId, Set.of(Role.CONGRESS_ADMIN)));

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepositoryPort).save(captor.capture());
        Room saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo(current.getName());
        assertThat(saved.getCapacity()).isEqualTo(current.getCapacity());
        assertThat(saved.getLocation()).isEqualTo(current.getLocation());
    }

    @Test
    void shouldClearLocationWhenBlank() {
        Room current = existingRoom();
        UUID actorId = current.getCreatedBy();

        when(roomRepositoryPort.findById(current.getId())).thenReturn(Optional.of(current));
        when(roomCongressPort.findManageableCongressById(current.getCongressId()))
                .thenReturn(Optional.of(congressSummary(current.getCongressId(), actorId)));
        when(roomRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomMapper.toResponse(any())).thenReturn(RoomResponse.builder().build());

        useCase.execute(
                current.getId(),
                UpdateRoomRequest.builder().location("   ").build(),
                requester(actorId, Set.of(Role.CONGRESS_ADMIN))
        );

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().getLocation()).isNull();
    }

    @Test
    void shouldRejectDuplicateNameInSameCongressExcludingCurrentId() {
        Room current = existingRoom();
        UUID actorId = current.getCreatedBy();

        when(roomRepositoryPort.findById(current.getId())).thenReturn(Optional.of(current));
        when(roomCongressPort.findManageableCongressById(current.getCongressId()))
                .thenReturn(Optional.of(congressSummary(current.getCongressId(), actorId)));
        when(roomRepositoryPort.existsByCongressIdAndNameAndIdNot(current.getCongressId(), "Sala A", current.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(
                current.getId(),
                UpdateRoomRequest.builder().name("Sala A").build(),
                requester(actorId, Set.of(Role.CONGRESS_ADMIN))
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("resource.conflict");
                });
    }

    @Test
    void shouldRejectCapacityLessOrEqualThanZero() {
        Room current = existingRoom();
        UUID actorId = current.getCreatedBy();

        when(roomRepositoryPort.findById(current.getId())).thenReturn(Optional.of(current));
        when(roomCongressPort.findManageableCongressById(current.getCongressId()))
                .thenReturn(Optional.of(congressSummary(current.getCongressId(), actorId)));

        assertThatThrownBy(() -> useCase.execute(
                current.getId(),
                UpdateRoomRequest.builder().capacity(0).build(),
                requester(actorId, Set.of(Role.CONGRESS_ADMIN))
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiException.getCode()).isEqualTo("validation.failed");
                });
    }

    @Test
    void shouldValidateScopedAccessForNonOwner() {
        Room current = existingRoom();
        UUID actorId = UUID.randomUUID();
        RoomCongressSummary congress = congressSummary(current.getCongressId(), current.getCreatedBy());

        when(roomRepositoryPort.findById(current.getId())).thenReturn(Optional.of(current));
        when(roomCongressPort.findManageableCongressById(current.getCongressId())).thenReturn(Optional.of(congress));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(actorId, congress.getInstitutionId(), "token")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(
                current.getId(),
                UpdateRoomRequest.builder().name("Nuevo").build(),
                requester(actorId, Set.of(Role.CONGRESS_ADMIN))
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
                });
    }

    @Test
    void shouldPropagateIamTechnicalFailure() {
        Room current = existingRoom();
        UUID actorId = UUID.randomUUID();
        RoomCongressSummary congress = congressSummary(current.getCongressId(), current.getCreatedBy());

        when(roomRepositoryPort.findById(current.getId())).thenReturn(Optional.of(current));
        when(roomCongressPort.findManageableCongressById(current.getCongressId())).thenReturn(Optional.of(congress));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(actorId, congress.getInstitutionId(), "token"))
                .thenThrow(CongressExceptions.iamUnavailable());

        assertThatThrownBy(() -> useCase.execute(
                current.getId(),
                UpdateRoomRequest.builder().name("Nuevo").build(),
                requester(actorId, Set.of(Role.CONGRESS_ADMIN))
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
    }

    @Test
    void shouldNotCallIamWhenRequesterIsOwner() {
        Room current = existingRoom();
        UUID actorId = current.getCreatedBy();

        when(roomRepositoryPort.findById(current.getId())).thenReturn(Optional.of(current));
        when(roomCongressPort.findManageableCongressById(current.getCongressId()))
                .thenReturn(Optional.of(congressSummary(current.getCongressId(), actorId)));
        when(roomRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomMapper.toResponse(any())).thenReturn(RoomResponse.builder().build());

        useCase.execute(current.getId(), UpdateRoomRequest.builder().build(), requester(actorId, Set.of(Role.CONGRESS_ADMIN)));

        verify(iamUserLookupPort, never()).isCongressAdminLinkedToInstitution(any(), any(), any());
    }

    private Room existingRoom() {
        return Room.builder()
                .id(UUID.randomUUID())
                .congressId(UUID.randomUUID())
                .name("Sala A")
                .capacity(100)
                .location("Edificio A")
                .createdBy(UUID.randomUUID())
                .build();
    }

    private RoomCongressSummary congressSummary(UUID congressId, UUID createdBy) {
        return RoomCongressSummary.builder()
                .id(congressId)
                .institutionId(UUID.randomUUID())
                .createdBy(createdBy)
                .build();
    }

    private RoomRequesterContext requester(UUID userId, Set<Role> roles) {
        return RoomRequesterContext.builder()
                .userId(userId)
                .roles(roles)
                .accessToken("token")
                .build();
    }
}
