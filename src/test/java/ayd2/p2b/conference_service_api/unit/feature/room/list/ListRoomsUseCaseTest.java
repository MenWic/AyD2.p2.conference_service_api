package ayd2.p2b.conference_service_api.unit.feature.room.list;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.room.application.list.ListRoomsUseCase;
import ayd2.p2b.conference_service_api.feature.room.application.port.RoomCongressPort;
import ayd2.p2b.conference_service_api.feature.room.application.port.RoomRepositoryPort;
import ayd2.p2b.conference_service_api.feature.room.domain.model.Room;
import ayd2.p2b.conference_service_api.feature.room.dto.response.RoomResponse;
import ayd2.p2b.conference_service_api.feature.room.mapper.RoomMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListRoomsUseCaseTest {

    @Mock
    private RoomRepositoryPort roomRepositoryPort;
    @Mock
    private RoomCongressPort roomCongressPort;
    @Mock
    private RoomMapper roomMapper;

    private ListRoomsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListRoomsUseCase(roomRepositoryPort, roomCongressPort, roomMapper);
    }

    @Test
    void shouldReturnPageResponseWithMappedItems() {
        UUID congressId = UUID.randomUUID();
        Room room = Room.builder().id(UUID.randomUUID()).congressId(congressId).name("Sala 1").build();
        RoomResponse mapped = RoomResponse.builder().id(room.getId()).name("Sala 1").build();

        when(roomCongressPort.existsPublicCongressById(congressId)).thenReturn(true);
        when(roomRepositoryPort.findPublicByCongressId(congressId, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(room), PageRequest.of(0, 20), 1));
        when(roomMapper.toResponse(room)).thenReturn(mapped);

        PageResponse<RoomResponse> response = useCase.execute(congressId, PageRequest.of(0, 20));

        assertThat(response.getItems()).containsExactly(mapped);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getTotalItems()).isEqualTo(1);
    }

    @Test
    void shouldReturnNotFoundWhenCongressIsMissingOrNotPublic() {
        UUID congressId = UUID.randomUUID();
        when(roomCongressPort.existsPublicCongressById(congressId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(congressId, PageRequest.of(0, 20)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiException.getCode()).isEqualTo("resource.not_found");
                });
    }
}
