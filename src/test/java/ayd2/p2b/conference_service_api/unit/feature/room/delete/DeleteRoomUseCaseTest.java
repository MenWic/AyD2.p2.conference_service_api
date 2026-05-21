package ayd2.p2b.conference_service_api.unit.feature.room.delete;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.congress.application.exception.CongressExceptions;
import ayd2.p2b.conference_service_api.feature.room.application.delete.DeleteRoomUseCase;
import ayd2.p2b.conference_service_api.feature.room.application.port.RoomCongressPort;
import ayd2.p2b.conference_service_api.feature.room.application.port.RoomDependencyPort;
import ayd2.p2b.conference_service_api.feature.room.application.port.RoomRepositoryPort;
import ayd2.p2b.conference_service_api.feature.room.domain.model.Room;
import ayd2.p2b.conference_service_api.feature.room.dto.internal.RoomCongressSummary;
import ayd2.p2b.conference_service_api.feature.room.dto.internal.RoomRequesterContext;
import ayd2.p2b.conference_service_api.feature.room.dto.response.RoomResponse;
import ayd2.p2b.conference_service_api.feature.room.mapper.RoomMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteRoomUseCaseTest {

    @Mock
    private RoomRepositoryPort roomRepositoryPort;
    @Mock
    private RoomCongressPort roomCongressPort;
    @Mock
    private RoomDependencyPort roomDependencyPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;
    @Mock
    private RoomMapper roomMapper;

    private DeleteRoomUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteRoomUseCase(
                roomRepositoryPort,
                roomCongressPort,
                roomDependencyPort,
                iamUserLookupPort,
                roomMapper
        );
    }

    @Test
    void shouldDeleteWhenNoActivitiesReferenceRoom() {
        Room room = existingRoom();
        UUID actorId = room.getCreatedBy();

        when(roomRepositoryPort.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomCongressPort.findManageableCongressById(room.getCongressId()))
                .thenReturn(Optional.of(congressSummary(room.getCongressId(), actorId)));
        when(roomDependencyPort.existsActivitiesForRoom(room.getId())).thenReturn(false);
        when(roomMapper.toResponse(any())).thenAnswer(invocation -> {
            Room deleted = invocation.getArgument(0);
            return RoomResponse.builder()
                    .id(deleted.getId())
                    .updatedAt(deleted.getUpdatedAt())
                    .build();
        });

        RoomResponse response = useCase.execute(room.getId(), requester(actorId, Set.of(Role.CONGRESS_ADMIN)));

        verify(roomRepositoryPort).deleteById(room.getId());
        verify(iamUserLookupPort, never()).isCongressAdminLinkedToInstitution(any(), any(), any());
        assertThat(response.getId()).isEqualTo(room.getId());
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldBlockDeleteWhenActivitiesReferenceRoom() {
        Room room = existingRoom();
        UUID actorId = room.getCreatedBy();

        when(roomRepositoryPort.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomCongressPort.findManageableCongressById(room.getCongressId()))
                .thenReturn(Optional.of(congressSummary(room.getCongressId(), actorId)));
        when(roomDependencyPort.existsActivitiesForRoom(room.getId())).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(room.getId(), requester(actorId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("resource.conflict");
                });
    }

    @Test
    void shouldValidateScopedAccessForNonOwner() {
        Room room = existingRoom();
        UUID actorId = UUID.randomUUID();
        RoomCongressSummary congress = congressSummary(room.getCongressId(), room.getCreatedBy());

        when(roomRepositoryPort.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomCongressPort.findManageableCongressById(room.getCongressId())).thenReturn(Optional.of(congress));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(actorId, congress.getInstitutionId(), "token")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(room.getId(), requester(actorId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
                });
    }

    @Test
    void shouldPropagateIamTechnicalFailure() {
        Room room = existingRoom();
        UUID actorId = UUID.randomUUID();
        RoomCongressSummary congress = congressSummary(room.getCongressId(), room.getCreatedBy());

        when(roomRepositoryPort.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomCongressPort.findManageableCongressById(room.getCongressId())).thenReturn(Optional.of(congress));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(actorId, congress.getInstitutionId(), "token"))
                .thenThrow(CongressExceptions.iamUnavailable());

        assertThatThrownBy(() -> useCase.execute(room.getId(), requester(actorId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
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
