package ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.room.application.port.RoomRepositoryPort;
import ayd2.p2b.conference_service_api.feature.room.domain.model.Room;
import ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.repository.RoomRepository;
import ayd2.p2b.conference_service_api.feature.room.mapper.RoomMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaRoomRepositoryAdapter implements RoomRepositoryPort {

    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;

    public JpaRoomRepositoryAdapter(RoomRepository roomRepository, RoomMapper roomMapper) {
        this.roomRepository = roomRepository;
        this.roomMapper = roomMapper;
    }

    @Override
    public Room save(Room room) {
        return roomMapper.toDomain(roomRepository.save(roomMapper.toEntity(room)));
    }

    @Override
    public Optional<Room> findById(UUID roomId) {
        return roomRepository.findById(roomId)
                .map(roomMapper::toDomain);
    }

    @Override
    public Optional<Room> findPublicById(UUID roomId) {
        return roomRepository.findPublicById(roomId)
                .map(roomMapper::toDomain);
    }

    @Override
    public Page<Room> findPublicByCongressId(UUID congressId, Pageable pageable) {
        return roomRepository.findPublicByCongressId(congressId, pageable)
                .map(roomMapper::toDomain);
    }

    @Override
    public boolean existsByCongressIdAndName(UUID congressId, String name) {
        return roomRepository.existsByCongressIdAndName(congressId, name);
    }

    @Override
    public boolean existsByCongressIdAndNameAndIdNot(UUID congressId, String name, UUID roomId) {
        return roomRepository.existsByCongressIdAndNameAndIdNot(congressId, name, roomId);
    }

    @Override
    public void deleteById(UUID roomId) {
        roomRepository.deleteById(roomId);
    }
}
