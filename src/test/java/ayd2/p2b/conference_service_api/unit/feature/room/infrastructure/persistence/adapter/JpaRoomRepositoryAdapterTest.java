package ayd2.p2b.conference_service_api.unit.feature.room.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.room.domain.model.Room;
import ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.adapter.JpaRoomRepositoryAdapter;
import ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.entity.RoomEntity;
import ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.repository.RoomRepository;
import ayd2.p2b.conference_service_api.feature.room.mapper.RoomMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaRoomRepositoryAdapterTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RoomMapper roomMapper;

    private JpaRoomRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaRoomRepositoryAdapter(roomRepository, roomMapper);
    }

    @Test
    void shouldSaveRoomThroughRepositoryAndMapper() {
        Room room = Room.builder().id(UUID.randomUUID()).name("Sala A").build();
        RoomEntity entity = new RoomEntity();
        RoomEntity savedEntity = new RoomEntity();
        savedEntity.setId(room.getId());

        when(roomMapper.toEntity(room)).thenReturn(entity);
        when(roomRepository.save(entity)).thenReturn(savedEntity);
        when(roomMapper.toDomain(savedEntity)).thenReturn(room);

        Room result = adapter.save(room);

        assertThat(result).isEqualTo(room);
    }

    @Test
    void shouldFindById() {
        UUID roomId = UUID.randomUUID();
        RoomEntity entity = new RoomEntity();
        Room room = Room.builder().id(roomId).build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(entity));
        when(roomMapper.toDomain(entity)).thenReturn(room);

        Optional<Room> result = adapter.findById(roomId);

        assertThat(result).contains(room);
    }

    @Test
    void shouldFindPublicById() {
        UUID roomId = UUID.randomUUID();
        RoomEntity entity = new RoomEntity();
        Room room = Room.builder().id(roomId).build();
        when(roomRepository.findPublicById(roomId)).thenReturn(Optional.of(entity));
        when(roomMapper.toDomain(entity)).thenReturn(room);

        Optional<Room> result = adapter.findPublicById(roomId);

        assertThat(result).contains(room);
    }

    @Test
    void shouldFindPublicByCongressId() {
        UUID congressId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        RoomEntity entity = new RoomEntity();
        Room room = Room.builder().id(UUID.randomUUID()).build();
        when(roomRepository.findPublicByCongressId(congressId, pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(roomMapper.toDomain(entity)).thenReturn(room);

        Page<Room> page = adapter.findPublicByCongressId(congressId, pageable);

        assertThat(page.getContent()).containsExactly(room);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldDelegateExistsChecksAndDelete() {
        UUID congressId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        when(roomRepository.existsByCongressIdAndName(congressId, "Sala A")).thenReturn(true);
        when(roomRepository.existsByCongressIdAndNameAndIdNot(congressId, "Sala B", roomId)).thenReturn(false);

        assertThat(adapter.existsByCongressIdAndName(congressId, "Sala A")).isTrue();
        assertThat(adapter.existsByCongressIdAndNameAndIdNot(congressId, "Sala B", roomId)).isFalse();

        adapter.deleteById(roomId);
        verify(roomRepository).deleteById(roomId);
        verify(roomRepository).existsByCongressIdAndName(congressId, "Sala A");
        verify(roomRepository).existsByCongressIdAndNameAndIdNot(congressId, "Sala B", roomId);
    }
}
