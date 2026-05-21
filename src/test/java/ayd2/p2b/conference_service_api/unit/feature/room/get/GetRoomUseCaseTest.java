package ayd2.p2b.conference_service_api.unit.feature.room.get;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.feature.room.application.get.GetRoomUseCase;
import ayd2.p2b.conference_service_api.feature.room.application.port.RoomRepositoryPort;
import ayd2.p2b.conference_service_api.feature.room.domain.model.Room;
import ayd2.p2b.conference_service_api.feature.room.dto.response.RoomResponse;
import ayd2.p2b.conference_service_api.feature.room.mapper.RoomMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRoomUseCaseTest {

    @Mock
    private RoomRepositoryPort roomRepositoryPort;
    @Mock
    private RoomMapper roomMapper;

    private GetRoomUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetRoomUseCase(roomRepositoryPort, roomMapper);
    }

    @Test
    void shouldReturnPublicRoom() {
        UUID roomId = UUID.randomUUID();
        Room room = Room.builder().id(roomId).name("Sala 1").build();
        RoomResponse response = RoomResponse.builder().id(roomId).name("Sala 1").build();

        when(roomRepositoryPort.findPublicById(roomId)).thenReturn(Optional.of(room));
        when(roomMapper.toResponse(room)).thenReturn(response);

        RoomResponse result = useCase.execute(roomId);

        assertThat(result.getId()).isEqualTo(roomId);
        assertThat(result.getName()).isEqualTo("Sala 1");
    }

    @Test
    void shouldReturnNotFoundForMissingRoom() {
        UUID roomId = UUID.randomUUID();
        when(roomRepositoryPort.findPublicById(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(roomId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiException.getCode()).isEqualTo("resource.not_found");
                });
    }
}
